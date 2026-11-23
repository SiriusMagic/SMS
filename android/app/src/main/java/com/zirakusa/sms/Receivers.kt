package com.zirakusa.sms

import android.app.Activity
import android.app.Service
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.provider.Telephony
import androidx.core.app.RemoteInput
import androidx.core.net.toUri
import com.zirakusa.sms.data.SharedValues
import com.zirakusa.sms.data.Settings

/**
 * A new SMS. As default app it arrives as SMS_DELIVER (and must be saved); otherwise as SMS_RECEIVED (Android saves it).
 * Either way the user gets a notification (with «Responder»).
 */
class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Only Android's own SMS broadcasts (the manifest also requires BROADCAST_SMS); anything else is ignored.
        if (intent.action != Telephony.Sms.Intents.SMS_DELIVER_ACTION && intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val book = AndroidBook(context)
        val deliver = intent.action == Telephony.Sms.Intents.SMS_DELIVER_ACTION
        if (!deliver && book.isDefault) return // the DELIVER copy handles it
        val parts = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        val address = parts.firstOrNull()?.originatingAddress ?: return
        val body = parts.joinToString("") { it.messageBody.orEmpty() }
        val at = parts.first().timestampMillis
        if (deliver) book.saveIncoming(address, body, at)
        Notifier.incoming(context, address, book.name(address), body, Settings(SharedValues(context)).preview)
        App.changed()
    }
}

/** MMS (pictures) aren't supported yet; Android requires a default SMS app to listen for them. */
class MmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.WAP_PUSH_DELIVER_ACTION) return
        App.changed() // nothing to save yet: the list just refreshes
    }
}

/** «Responder con un mensaje» when rejecting a call (asked of default SMS apps). */
class RespondService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val to = intent?.data?.schemeSpecificPart?.substringBefore('?')
        val text = intent?.getStringExtra(Intent.EXTRA_TEXT)
        if (!to.isNullOrBlank() && !text.isNullOrBlank()) AndroidBook(this).send(to, text)
        stopSelf(startId)
        return START_NOT_STICKY
    }
}

/** How a sent message went: as default app, a failure is written in the store. */
class SentReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val uri = intent.getStringExtra(URI)
        if (resultCode != Activity.RESULT_OK && uri != null) runCatching {
            context.contentResolver.update(uri.toUri(), ContentValues().apply { put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_FAILED) }, null, null)
        }
        App.changed()
    }
    companion object { const val URI = "uri" }
}

/** The buttons of the notifications: «Responder» (typed in the notification) and «Enviar» of a message the AI proposed. */
class ActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val to = intent.getStringExtra(Notifier.TO) ?: return
        val text = when (intent.action) {
            Notifier.REPLY -> RemoteInput.getResultsFromIntent(intent)?.getCharSequence(Notifier.TEXT)?.toString()
            Notifier.SEND -> intent.getStringExtra(Notifier.TEXT)
            else -> null
        }?.takeIf { it.isNotBlank() } ?: return
        AndroidBook(context).send(to, text)
        Notifier.cancel(context, intent.getIntExtra(Notifier.ID, 0))
        App.changed()
    }
}
