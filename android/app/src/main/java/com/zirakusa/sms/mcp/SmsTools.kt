package com.zirakusa.sms.mcp

import com.zirakusa.sms.data.Message
import com.zirakusa.sms.data.NoAccess
import com.zirakusa.sms.data.Numbers
import com.zirakusa.sms.data.Sms
import com.zirakusa.sms.data.SmsBook
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId

/** What the tools read and propose; the app gives Android's SMS store and a notification, tests fakes. */
interface SmsData {
    val book: SmsBook
    fun zone(): ZoneId = ZoneId.systemDefault()
    /** Shows «[who] quiere enviar a …» with «Enviar»; false when notifications are off. */
    fun propose(who: String, to: String, name: String, text: String): Boolean
}

/** An expected failure the AI can read (structuredContent.error.code), never a crash. */
class ToolFailure(val code: String, message: String) : Exception(message)

/**
 * SMS's MCP tools: conversations, messages, search and unread; with a read-and-write token the AI may *propose* a message
 * (the user taps «Enviar»). Verification codes are always hidden (••••••): a leaked token must never open someone's
 * accounts. The AI never sends by itself.
 */
object SmsTools {
    class Tool(val name: String, val description: String, val write: Boolean, val schema: JSONObject)
    const val TEXT_MAX = 1000

    private fun schema(required: List<String>, vararg props: Pair<String, JSONObject>) = JSONObject()
        .put("type", "object").put("properties", JSONObject().apply { props.forEach { (k, v) -> put(k, v) } })
        .put("required", JSONArray(required)).put("additionalProperties", false)
    private fun prop(type: String, description: String) = JSONObject().put("type", type).put("description", description)

    val tools = listOf(
        Tool("sms_threads", "Conversations, newest first: who (saved name or number), the last message and how many are unread.", false,
            schema(emptyList(), "limit" to prop("integer", "1–50, default 15."))),
        Tool("sms_messages", "The messages of one conversation, newest last.", false,
            schema(listOf("thread_id"), "thread_id" to prop("integer", "From sms_threads."), "limit" to prop("integer", "1–100, default 30."))),
        Tool("sms_search", "Messages containing some words (accents ignored), newest first.", false,
            schema(listOf("query"), "query" to prop("string", "2–100 characters."), "limit" to prop("integer", "1–50, default 20."))),
        Tool("sms_unread", "Unread received messages.", false, schema(emptyList())),
        Tool("sms_send_request", "Proposes a message: the phone shows it with «Enviar»; nothing is sent until the user taps it.", true,
            schema(listOf("to", "text"), "to" to prop("string", "A number."), "text" to prop("string", "Up to $TEXT_MAX characters."))),
    )

    fun available(access: Access) = tools.filter { !it.write || access == Access.WRITE }

    fun call(name: String, args: JSONObject, access: Access, data: SmsData, who: String): JSONObject {
        val tool = tools.firstOrNull { it.name == name } ?: throw ToolFailure("UNKNOWN_TOOL", "No tool named $name. (SMS never sends for the AI.)")
        if (tool.write && access != Access.WRITE) throw ToolFailure("READ_ONLY", "This token can only read. Ask for a read-and-write token.")
        val known = tool.schema.getJSONObject("properties").keys().asSequence().toSet()
        args.keys().forEach { if (it !in known) throw ToolFailure("BAD_ARGUMENT", "Unknown argument «$it».") }
        val b = data.book
        try {
            return when (name) {
                "sms_threads" -> {
                    val limit = int(args, "limit", 15, 1..50)
                    JSONObject().put("threads", JSONArray(Sms.threads(b.messages(), b::name).take(limit).map { t ->
                        JSONObject().put("thread_id", t.id).put("who", t.title).put("number", t.address).put("unread", t.unread).put("count", t.count)
                            .put("last", message(t.last, data.zone()))
                    }))
                }
                "sms_messages" -> {
                    val thread = (args.opt("thread_id") as? Int)?.toLong() ?: (args.opt("thread_id") as? Long) ?: throw ToolFailure("BAD_ARGUMENT", "«thread_id» is a whole number.")
                    val limit = int(args, "limit", 30, 1..100)
                    val all = Sms.conversation(b.messages(), thread)
                    if (all.isEmpty()) throw ToolFailure("NOT_FOUND", "No conversation with id $thread.")
                    JSONObject().put("who", b.name(all.last().address).ifBlank { all.last().address }).put("messages", JSONArray(all.takeLast(limit).map { message(it, data.zone()) }))
                }
                "sms_search" -> {
                    val q = (args.opt("query") as? String)?.trim() ?: throw ToolFailure("BAD_ARGUMENT", "«query» is text.")
                    if (q.length !in 2..100) throw ToolFailure("BAD_ARGUMENT", "«query» has 2–100 characters.")
                    // Searched in the masked text: looking for a code finds nothing.
                    val qp = Sms.plain(q)
                    val found = b.messages().filter { Sms.plain(Sms.masked(it.body)).contains(qp) || Sms.plain(it.address).contains(qp) }.sortedByDescending { it.at }
                    JSONObject().put("total", found.size).put("messages", JSONArray(found.take(int(args, "limit", 20, 1..50)).map { message(it, data.zone()).put("thread_id", it.thread) }))
                }
                "sms_unread" -> {
                    val unread = b.messages().filter { !it.read && !it.mine }
                    JSONObject().put("total", unread.size).put("messages", JSONArray(unread.take(50).map { message(it, data.zone()).put("thread_id", it.thread).put("who", b.name(it.address).ifBlank { it.address }) }))
                }
                "sms_send_request" -> {
                    val to = (args.opt("to") as? String)?.let(Numbers::clean)?.takeIf(Numbers::valid) ?: throw ToolFailure("BAD_ARGUMENT", "«to» is a number (3–15 digits).")
                    val text = (args.opt("text") as? String)?.trim()?.takeIf { it.isNotEmpty() && it.length <= TEXT_MAX } ?: throw ToolFailure("BAD_ARGUMENT", "«text» has 1–$TEXT_MAX characters.")
                    val saved = b.name(to)
                    if (!data.propose(who, to, saved, text)) throw ToolFailure("NOTIFICATIONS_OFF", "SMS's notifications are off on the phone; the user can't see the proposal.")
                    val (parts, _) = Sms.parts(text)
                    JSONObject().put("proposed", true).put("to", to).put("name", saved).put("sms_parts", parts).put("note", "Nothing is sent until the user taps «Enviar».")
                }
                else -> throw ToolFailure("UNKNOWN_TOOL", "No tool named $name.")
            }
        } catch (_: NoAccess) { throw ToolFailure("NO_PERMISSION", "The user hasn't allowed SMS to read the messages. Ask them to open SMS once.") }
    }

    private fun message(m: Message, zone: ZoneId) = JSONObject().put("id", m.id).put("from_me", m.mine).put("box", m.box.key)
        .put("text", Sms.masked(m.body)).put("read", m.read).put("at", Instant.ofEpochMilli(m.at).atZone(zone).toLocalDateTime().withNano(0).toString())

    private fun int(args: JSONObject, key: String, default: Int, range: IntRange): Int {
        if (!args.has(key)) return default
        val n = (args.opt(key) as? Int) ?: throw ToolFailure("BAD_ARGUMENT", "«$key» is a whole number.")
        if (n !in range) throw ToolFailure("BAD_ARGUMENT", "«$key» is ${range.first}–${range.last}.")
        return n
    }
}
