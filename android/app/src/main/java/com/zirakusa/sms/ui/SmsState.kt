package com.zirakusa.sms.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.zirakusa.sms.data.KeyValues
import com.zirakusa.sms.data.Message
import com.zirakusa.sms.data.NoAccess
import com.zirakusa.sms.data.Numbers
import com.zirakusa.sms.data.Settings
import com.zirakusa.sms.data.Sms
import com.zirakusa.sms.data.SmsBook
import com.zirakusa.sms.ui.theme.Accent
import com.zirakusa.sms.ui.theme.ThemeMode

enum class Page { THREADS, CONVERSATION, NEW, SETTINGS, MCP }

/** The only owner of SMS's state: the messages, the conversation open, what is being written and the settings. Screens only paint. */
class SmsState(kv: KeyValues, private val book: SmsBook, private val isDefault: () -> Boolean = { false }) {
    private val settings = Settings(kv)

    var page by mutableStateOf(Page.THREADS)
    var messages by mutableStateOf(emptyList<Message>()); private set
    var allowed by mutableStateOf(true); private set
    var defaultApp by mutableStateOf(false); private set
    var query by mutableStateOf("")
    /** The number of the conversation open (also a new one without messages yet). */
    var address by mutableStateOf(""); private set
    var draft by mutableStateOf("")
    var to by mutableStateOf("")
    var failedToSend by mutableStateOf(false); private set

    var preview by mutableStateOf(settings.preview); private set
    var themeMode by mutableStateOf(settings.theme?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.BLACK); private set
    var accent by mutableStateOf(settings.accent?.let { runCatching { Accent.valueOf(it) }.getOrNull() } ?: Accent.AZUL); private set

    val threads get() = Sms.threads(messages, book::name)
    val found get() = Sms.search(messages, query)
    /** The open conversation, oldest first: every message with the same number (099… and +593… alike). */
    val conversation get() = Numbers.key(address).let { k -> messages.filter { Numbers.key(it.address) == k && k.isNotEmpty() || (k.isEmpty() && it.address == address) }.sortedBy { it.at } }
    val title get() = book.name(address).ifBlank { address }
    val unread get() = messages.count { !it.read && !it.mine }
    val parts get() = Sms.parts(draft)

    fun load() {
        defaultApp = isDefault()
        try { messages = book.messages(); allowed = true } catch (_: NoAccess) { messages = emptyList(); allowed = false }
    }

    fun open(number: String, text: String? = null) {
        address = number; page = Page.CONVERSATION; failedToSend = false
        if (text != null) draft = text
        conversation.firstOrNull()?.let { book.markRead(it.thread) }
        load()
    }

    /** «Nuevo mensaje» › a number typed. */
    fun startNew() { if (Numbers.valid(to)) { open(Numbers.clean(to)); to = "" } }

    fun send(): Boolean {
        val text = draft.trim()
        if (text.isEmpty() || address.isBlank()) return false
        val ok = book.send(address, text)
        failedToSend = !ok
        if (ok) draft = ""
        load()
        return ok
    }

    fun back() { page = when (page) { Page.MCP -> Page.SETTINGS; else -> Page.THREADS }; if (page == Page.THREADS) load() }

    fun changePreview(on: Boolean) { preview = on; settings.preview = on }
    fun changeTheme(m: ThemeMode) { themeMode = m; settings.theme = m.name }
    fun changeAccent(a: Accent) { accent = a; settings.accent = a.name }
}
