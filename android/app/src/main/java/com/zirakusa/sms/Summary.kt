package com.zirakusa.sms

import android.content.ContentProvider
import android.content.ContentValues
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import com.zirakusa.sms.data.Message
import com.zirakusa.sms.data.Sms

/** The lines of «Hoy» for the family summary: unread conversations («Ana Pérez · ¿Vamos mañana?»), codes hidden. Pure. */
object SummaryLines {
    const val MAX = 5
    fun lines(messages: List<Message>, name: (String) -> String): List<String> =
        Sms.threads(messages.filter { !it.read && !it.mine }, name).take(MAX).map { t ->
            "${t.title}${if (t.unread > 1) " (${t.unread})" else ""} · ${Sms.masked(t.last.body).replace('\n', ' ').take(60)}"
        }
}

/** The family summary (see Inicio's FAMILIA.md): unread messages for Inicio's «Hoy». Only apps signed by the same developer may ask. */
class SummaryProvider : ContentProvider() {
    override fun onCreate() = true
    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        if (method != "summary") return null
        val context = context ?: return null
        val caller = callingPackage ?: return null
        if (context.packageManager.checkSignatures(context.packageName, caller) != PackageManager.SIGNATURE_MATCH) return null
        val book = AndroidBook(context)
        val lines = runCatching { SummaryLines.lines(book.messages(300), book::name) }.getOrDefault(emptyList())
        return Bundle().apply {
            putString("title", context.getString(R.string.summary_title))
            putStringArrayList("lines", ArrayList(lines))
            putString("empty", context.getString(R.string.summary_empty))
        }
    }
    override fun query(uri: Uri, p: Array<out String>?, s: String?, a: Array<out String>?, o: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, s: String?, a: Array<out String>?) = 0
    override fun update(uri: Uri, v: ContentValues?, s: String?, a: Array<out String>?) = 0
}
