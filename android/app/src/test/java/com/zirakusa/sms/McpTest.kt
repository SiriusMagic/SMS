package com.zirakusa.sms

import com.zirakusa.sms.data.MemoryBook
import com.zirakusa.sms.mcp.Access
import com.zirakusa.sms.mcp.HubLink
import com.zirakusa.sms.mcp.Kind
import com.zirakusa.sms.mcp.McpHandler
import com.zirakusa.sms.mcp.SmsData
import com.zirakusa.sms.mcp.Token
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.ZoneOffset

/** SMS's MCP: tools list (no sending), codes always hidden, the proposal, bad arguments, not found, no permission, read-only, tokens, hub link. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class McpTest {
    private val memory = MemoryBook(Inbox.all, Inbox.names)
    private val data = object : SmsData {
        override val book = memory
        val proposed = mutableListOf<List<String>>()
        var notifications = true
        override fun zone() = ZoneOffset.UTC
        override fun propose(who: String, to: String, name: String, text: String) = notifications.also { if (it) proposed += listOf(who, to, name, text) }
    }
    private val writer = Token("w", "Claude", "sms_" + "w".repeat(40), Access.WRITE, Kind.HUB, "SMS")
    private val reader = Token("r", "Lector", "sms_" + "r".repeat(40), Access.READ)
    private val paused = Token("p", "Viejo", "sms_" + "p".repeat(40), Access.WRITE, paused = true)
    private val handler = McpHandler({ listOf(writer, reader, paused) }, data, "0.1.0")

    private fun rpc(token: Token, method: String, params: JSONObject = JSONObject()) = runBlocking {
        JSONObject(handler.handle("POST", "/mcp", mapOf("Authorization" to "Bearer ${token.secret}"), emptyMap(),
            JSONObject().put("jsonrpc", "2.0").put("id", 1).put("method", method).put("params", params).toString()).body).getJSONObject("result")
    }
    private fun tool(name: String, args: JSONObject, token: Token = writer) = rpc(token, "tools/call", JSONObject().put("name", name).put("arguments", args))
    private fun ok(name: String, args: JSONObject = JSONObject()) =
        tool(name, args).also { assertFalse(it.toString(), it.optBoolean("isError")) }.getJSONObject("structuredContent")
    private fun code(name: String, args: JSONObject = JSONObject(), token: Token = writer) = tool(name, args, token).also { assertTrue(it.optBoolean("isError")) }
        .getJSONObject("structuredContent").getJSONObject("error").getString("code")
    private fun names(token: Token) = rpc(token, "tools/list").getJSONArray("tools").let { t -> (0 until t.length()).map { t.getJSONObject(it).getString("name") } }

    @Test fun listsTheToolsWithoutSending() {
        assertEquals(listOf("sms_threads", "sms_messages", "sms_search", "sms_unread", "sms_send_request"), names(writer))
        assertEquals(listOf("sms_threads", "sms_messages", "sms_search", "sms_unread"), names(reader))
        assertEquals("UNKNOWN_TOOL", code("sms_send", JSONObject().put("to", "0991234567").put("text", "x")))
        assertEquals("sms", rpc(writer, "initialize").getJSONObject("serverInfo").getString("name"))
    }

    @Test fun readsWithCodesHidden() {
        val threads = ok("sms_threads").getJSONArray("threads")
        assertEquals("Ana Pérez", threads.getJSONObject(0).getString("who")); assertEquals(2, threads.getJSONObject(0).getInt("unread"))
        val bank = threads.getJSONObject(1).getJSONObject("last").getString("text")
        assertFalse(bank.contains("482913")); assertTrue(bank.contains("••••••"))
        assertEquals(3, ok("sms_messages", JSONObject().put("thread_id", 10)).getJSONArray("messages").length())
        assertFalse(ok("sms_messages", JSONObject().put("thread_id", 20)).toString().contains("482913"))
        assertFalse(ok("sms_unread").toString().contains("482913")); assertEquals(3, ok("sms_unread").getInt("total"))
        assertEquals(0, ok("sms_search", JSONObject().put("query", "482913")).getInt("total"))  // searching a code finds nothing
        assertEquals(1, ok("sms_search", JSONObject().put("query", "cine")).getInt("total"))
        assertFalse(tool("sms_unread", JSONObject()).toString().contains("482913"))               // nor in the text content
    }

    @Test fun proposesNeverSends() {
        val p = ok("sms_send_request", JSONObject().put("to", "+593 99 123 4567").put("text", "Llego en 10 minutos"))
        assertTrue(p.getBoolean("proposed")); assertEquals("Ana Pérez", p.getString("name")); assertEquals(1, p.getInt("sms_parts"))
        assertEquals(listOf("Claude", "+593991234567", "Ana Pérez", "Llego en 10 minutos"), data.proposed.single())
        assertTrue(memory.sent.isEmpty())                                                       // nothing sent
    }

    @Test fun badArgumentsAreRefused() {
        assertEquals("BAD_ARGUMENT", code("sms_threads", JSONObject().put("limit", 0)))
        assertEquals("BAD_ARGUMENT", code("sms_messages", JSONObject().put("thread_id", "10")))
        assertEquals("BAD_ARGUMENT", code("sms_search", JSONObject().put("query", "a")))
        assertEquals("BAD_ARGUMENT", code("sms_send_request", JSONObject().put("to", "12").put("text", "x")))
        assertEquals("BAD_ARGUMENT", code("sms_send_request", JSONObject().put("to", "0991234567").put("text", " ")))
        assertEquals("BAD_ARGUMENT", code("sms_send_request", JSONObject().put("to", "0991234567").put("text", "x".repeat(1001))))
        assertEquals("BAD_ARGUMENT", code("sms_send_request", JSONObject().put("to", "0991234567").put("text", "x").put("now", true)))
        assertTrue(data.proposed.isEmpty())
    }

    @Test fun notFoundNoPermissionNotificationsAndReadOnly() {
        assertEquals("NOT_FOUND", code("sms_messages", JSONObject().put("thread_id", 99)))
        assertEquals("READ_ONLY", code("sms_send_request", JSONObject().put("to", "0991234567").put("text", "x"), reader))
        data.notifications = false
        assertEquals("NOTIFICATIONS_OFF", code("sms_send_request", JSONObject().put("to", "0991234567").put("text", "x")))
        memory.allowed = false
        assertEquals("NO_PERMISSION", code("sms_threads"))
    }

    @Test fun wrongAndPausedTokensAndHubLink() = runBlocking {
        val body = """{"jsonrpc":"2.0","id":1,"method":"tools/list"}"""
        assertEquals(401, handler.handle("POST", "/mcp", mapOf("Authorization" to "Bearer ${paused.secret}"), emptyMap(), body).status)
        assertEquals(401, handler.handle("POST", "/mcp", mapOf("Authorization" to "Bearer sms_wrong"), emptyMap(), body).status)
        assertTrue(Token.secret().startsWith("sms_"))
        assertEquals("NOT_SAME_DEVELOPER", HubLink.answer(false).getString("error"))
        assertEquals("REDIRECT_OFF", HubLink.answer(true).getString("error"))
    }
}
