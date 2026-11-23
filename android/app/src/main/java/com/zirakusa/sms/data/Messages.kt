package com.zirakusa.sms.data

import java.text.Normalizer

/** Where a message is. [key] is what MCP uses. */
enum class Box(val key: String) { INBOX("received"), SENT("sent"), OUTBOX("sending"), FAILED("failed"), DRAFT("draft") }

/** One SMS. [thread] groups a conversation (Android's thread id). */
data class Message(val id: Long, val thread: Long, val address: String, val body: String, val at: Long, val box: Box, val read: Boolean = true) {
    val mine get() = box != Box.INBOX
}

/** A conversation: its last message, how many are unread and the name of the other person (when saved). */
data class Thread(val id: Long, val address: String, val name: String, val last: Message, val unread: Int, val count: Int) {
    val title get() = name.ifBlank { address }
}

/** Pure rules of the conversations: grouping, search, verification codes and how many SMS a text takes. */
object Sms {
    /** Conversations, newest first. [names] gives a saved name for an address. */
    fun threads(messages: List<Message>, names: (String) -> String = { "" }): List<Thread> =
        messages.groupBy { it.thread }.map { (id, ms) ->
            val last = ms.maxBy { it.at }
            Thread(id, last.address, names(last.address), last, ms.count { !it.read && !it.mine }, ms.size)
        }.sortedByDescending { it.last.at }

    /** Messages of one conversation, oldest first (as they are read). */
    fun conversation(messages: List<Message>, thread: Long) = messages.filter { it.thread == thread }.sortedBy { it.at }

    fun search(messages: List<Message>, query: String): List<Message> {
        val q = plain(query.trim())
        if (q.length < 2) return emptyList()
        return messages.filter { plain(it.body).contains(q) || plain(it.address).contains(q) }.sortedByDescending { it.at }
    }

    private val codeWords = Regex("(c[oó]digo|code|verificaci[oó]n|verification|otp|pin|contrase[nñ]a|password|clave|token|2fa|autenticaci[oó]n)", RegexOption.IGNORE_CASE)
    // Not part of a longer number or a decimal (12.50): a sentence's full stop after the code is fine.
    private val digits = Regex("(?<!\\d)(?<!\\d[.,])(\\d{4,8}|\\d{3}[- ]\\d{3})(?!\\d)(?![.,]\\d)")

    /** The verification code in a message («Tu código es 482913»), or null. Only with a code word nearby, to skip prices or dates. */
    fun code(body: String): String? {
        if (!codeWords.containsMatchIn(body)) return null
        return digits.find(body)?.value?.filter { it.isDigit() }
    }

    /** The text with any verification code hidden: what leaves the app (MCP, Inicio). Every code-like number goes. */
    fun masked(body: String): String =
        if (!codeWords.containsMatchIn(body)) body else digits.replace(body) { m -> "•".repeat(m.value.count { it.isDigit() }) }

    /**
     * How many SMS a text takes and how many characters are left in the last: 160 (or 153 per part) with the basic
     * alphabet, 70 (67 per part) when it has other characters (emojis, some accents like «ú» are fine, «ç» too).
     */
    fun parts(text: String): Pair<Int, Int> {
        val gsm = text.all { it in GSM || it in GSM_EXT }
        val units = if (gsm) text.sumOf { if (it in GSM_EXT) 2L else 1L }.toInt() else text.length
        val (one, many) = if (gsm) 160 to 153 else 70 to 67
        if (units <= one) return 1 to (one - units)
        val n = (units + many - 1) / many
        return n to (n * many - units)
    }

    fun plain(s: String) = Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")

    private const val GSM = "@£\$¥èéùìòÇ\nØø\rÅåΔ_ΦΓΛΩΠΨΣΘΞÆæßÉ !\"#¤%&'()*+,-./0123456789:;<=>?¡ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÑÜ§¿abcdefghijklmnopqrstuvwxyzäöñüà"
    private const val GSM_EXT = "^{}\\[~]|€"
}

/** Numbers the Ecuadorian way: «099 123 4567» and «+593 99 123 4567» are the same. */
object Numbers {
    fun key(number: String): String {
        var d = number.filter { it.isDigit() }
        if (d.startsWith("593") && d.length >= 11) d = d.drop(3)
        return d.trimStart('0').takeLast(9)
    }
    fun clean(number: String) = number.trim().let { t -> (if (t.startsWith("+")) "+" else "") + t.filter { it.isDigit() } }
    /** A number someone can write to: at least 3 digits (short codes like 611 too). */
    fun valid(number: String) = clean(number).count { it.isDigit() } in 3..15
}
