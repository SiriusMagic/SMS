package com.zirakusa.sms.data

import androidx.core.content.edit

/** Where the messages are: Android's SMS store in the app, a list in the tests. */
interface SmsBook {
    /** The newest messages; throws [NoAccess] without the permission. */
    fun messages(limit: Int = 2000): List<Message>
    /** The saved contact name of an address, or "". */
    fun name(address: String): String
    fun markRead(thread: Long)
    /** Sends a text; the result comes later as a [Message] in the store. False when it couldn't even start. */
    fun send(address: String, text: String): Boolean
}

/** The user hasn't allowed SMS to read the messages. */
class NoAccess : Exception()

class MemoryBook(items: List<Message> = emptyList(), private val names: Map<String, String> = emptyMap()) : SmsBook {
    val items = items.toMutableList()
    var allowed = true
    val sent = mutableListOf<Pair<String, String>>()
    override fun messages(limit: Int): List<Message> { if (!allowed) throw NoAccess(); return items.sortedByDescending { it.at }.take(limit) }
    override fun name(address: String) = names.entries.firstOrNull { Numbers.key(it.key) == Numbers.key(address) }?.value.orEmpty()
    override fun markRead(thread: Long) { items.replaceAll { if (it.thread == thread) it.copy(read = true) else it } }
    override fun send(address: String, text: String): Boolean {
        sent += address to text
        val thread = items.firstOrNull { Numbers.key(it.address) == Numbers.key(address) }?.thread ?: ((items.maxOfOrNull { it.thread } ?: 0) + 1)
        items += Message((items.maxOfOrNull { it.id } ?: 0) + 1, thread, address, text, (items.maxOfOrNull { it.at } ?: 0) + 1000, Box.SENT)
        return true
    }
}

/** Where the settings live: the phone's preferences in the app, a map in the tests. */
interface KeyValues {
    fun get(key: String): String?
    fun put(key: String, value: String?)
}

class MapValues(private val map: MutableMap<String, String> = mutableMapOf()) : KeyValues {
    override fun get(key: String) = map[key]
    override fun put(key: String, value: String?) { if (value == null) map.remove(key) else map[key] = value }
}

/** Every setting of SMS, typed. */
class Settings(private val kv: KeyValues) {
    private fun flag(key: String, default: Boolean) = kv.get(key)?.let { it == "1" } ?: default
    /** New messages show their text in the notification (off: only who wrote). */
    var preview: Boolean get() = flag("preview", true); set(v) = kv.put("preview", if (v) "1" else "0")
    var theme: String? get() = kv.get("theme"); set(v) = kv.put("theme", v)
    var accent: String? get() = kv.get("accent"); set(v) = kv.put("accent", v)
}

/** The phone's preferences file «sms» (only settings and tokens: messages stay in Android's own store). */
class SharedValues(context: android.content.Context) : KeyValues {
    private val prefs = context.applicationContext.getSharedPreferences("sms", android.content.Context.MODE_PRIVATE)
    override fun get(key: String): String? = prefs.getString(key, null)
    override fun put(key: String, value: String?) {
        prefs.edit(commit = true) { if (value == null) remove(key) else putString(key, value) }
    }
}
