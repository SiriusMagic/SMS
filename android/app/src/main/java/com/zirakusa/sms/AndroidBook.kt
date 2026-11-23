package com.zirakusa.sms

import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Telephony
import android.telephony.SmsManager
import com.zirakusa.sms.data.Box
import com.zirakusa.sms.data.Message
import com.zirakusa.sms.data.NoAccess
import com.zirakusa.sms.data.SmsBook

/**
 * The phone's SMS store (always with `?` arguments) and sending through Android. When SMS is the default app it also
 * writes what it sends and receives, as Android asks of default apps; otherwise Android keeps the store itself.
 */
class AndroidBook(context: Context) : SmsBook {
    private val app = context.applicationContext
    private val cr = app.contentResolver
    private val names = HashMap<String, String>()

    val isDefault get() = Telephony.Sms.getDefaultSmsPackage(app) == app.packageName

    override fun messages(limit: Int): List<Message> {
        val cols = arrayOf(Telephony.Sms._ID, Telephony.Sms.THREAD_ID, Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE, Telephony.Sms.TYPE, Telephony.Sms.READ)
        val cursor = try { cr.query(Telephony.Sms.CONTENT_URI, cols, null, null, Telephony.Sms.DATE + " DESC") } catch (_: SecurityException) { throw NoAccess() }
        val out = ArrayList<Message>()
        cursor?.use { c ->
            while (c.moveToNext() && out.size < limit) {
                out += Message(c.getLong(0), c.getLong(1), c.getString(2).orEmpty(), c.getString(3).orEmpty(), c.getLong(4), box(c.getInt(5)), c.getInt(6) == 1)
            }
        }
        return out
    }

    override fun name(address: String): String = names.getOrPut(address) {
        if (address.isBlank()) return@getOrPut ""
        runCatching {
            cr.query(Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(address)), arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null)
                ?.use { if (it.moveToFirst()) it.getString(0).orEmpty() else "" }.orEmpty()
        }.getOrDefault("")
    }

    /** Only the default SMS app may change the store; otherwise Android ignores it. */
    override fun markRead(thread: Long) {
        if (!isDefault) return
        runCatching { cr.update(Telephony.Sms.CONTENT_URI, ContentValues().apply { put(Telephony.Sms.READ, 1) },
            Telephony.Sms.THREAD_ID + "=? AND " + Telephony.Sms.READ + "=0", arrayOf(thread.toString())) }
    }

    override fun send(address: String, text: String): Boolean = runCatching {
        val sms = app.getSystemService(SmsManager::class.java) ?: return false
        val saved = if (isDefault) cr.insert(Telephony.Sms.Sent.CONTENT_URI, ContentValues().apply {
            put(Telephony.Sms.ADDRESS, address); put(Telephony.Sms.BODY, text); put(Telephony.Sms.DATE, System.currentTimeMillis()); put(Telephony.Sms.READ, 1)
        }) else null
        val parts = sms.divideMessage(text)
        val sent = Intent(app, SentReceiver::class.java).putExtra(SentReceiver.URI, saved?.toString())
        val pi = PendingIntent.getBroadcast(app, (System.nanoTime() % Int.MAX_VALUE).toInt(), sent, PendingIntent.FLAG_IMMUTABLE)
        sms.sendMultipartTextMessage(address, null, parts, ArrayList(List(parts.size) { pi }), null)
        true
    }.getOrDefault(false)

    /** The default app writes what arrives (Android does it for the others). */
    fun saveIncoming(address: String, body: String, at: Long) {
        if (!isDefault) return
        runCatching { cr.insert(Telephony.Sms.Inbox.CONTENT_URI, ContentValues().apply {
            put(Telephony.Sms.ADDRESS, address); put(Telephony.Sms.BODY, body); put(Telephony.Sms.DATE, at); put(Telephony.Sms.READ, 0)
        }) }
    }

    private fun box(t: Int) = when (t) {
        Telephony.Sms.MESSAGE_TYPE_INBOX -> Box.INBOX; Telephony.Sms.MESSAGE_TYPE_SENT -> Box.SENT; Telephony.Sms.MESSAGE_TYPE_OUTBOX, Telephony.Sms.MESSAGE_TYPE_QUEUED -> Box.OUTBOX
        Telephony.Sms.MESSAGE_TYPE_FAILED -> Box.FAILED; Telephony.Sms.MESSAGE_TYPE_DRAFT -> Box.DRAFT; else -> Box.INBOX
    }
}
