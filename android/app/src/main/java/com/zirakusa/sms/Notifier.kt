package com.zirakusa.sms

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import com.zirakusa.sms.data.Sms

/** SMS's notifications: a new message (with «Responder»), and a message the AI proposes (with «Enviar» and «Editar»). */
object Notifier {
    const val TO = "to"; const val TEXT = "text"; const val ID = "id"
    const val REPLY = "com.zirakusa.sms.REPLY"; const val SEND = "com.zirakusa.sms.SEND"
    private const val MESSAGES = "messages"; private const val REQUESTS = "requests"

    fun allowed(context: Context) = permitted(context) && NotificationManagerCompat.from(context).areNotificationsEnabled()
    private fun permitted(context: Context) = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun channels(context: Context) = context.getSystemService(NotificationManager::class.java)?.run {
        createNotificationChannel(NotificationChannel(MESSAGES, context.getString(R.string.channel_messages), NotificationManager.IMPORTANCE_HIGH))
        createNotificationChannel(NotificationChannel(REQUESTS, context.getString(R.string.channel_requests), NotificationManager.IMPORTANCE_HIGH))
    }

    private fun open(context: Context, to: String, draft: String? = null, code: Int) = PendingIntent.getActivity(context, code,
        Intent(context, MainActivity::class.java).putExtra(MainActivity.ADDRESS, to).putExtra(MainActivity.DRAFT, draft)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    fun incoming(context: Context, address: String, name: String, body: String, preview: Boolean) {
        if (!allowed(context)) return
        channels(context)
        val id = address.hashCode()
        val reply = RemoteInput.Builder(TEXT).setLabel(context.getString(R.string.reply)).build()
        // Mutable so Android can put the typed reply in; explicit to our own receiver.
        val replyIntent = PendingIntent.getBroadcast(context, id, Intent(context, ActionReceiver::class.java).setAction(REPLY).putExtra(TO, address).putExtra(ID, id),
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val code = Sms.code(body)
        val n = NotificationCompat.Builder(context, MESSAGES).setSmallIcon(R.drawable.ic_stat_sms).setContentTitle(name.ifBlank { address })
            .setContentText(if (preview) body else context.getString(R.string.new_message)).setStyle(if (preview) NotificationCompat.BigTextStyle().bigText(body) else null)
            .setContentIntent(open(context, address, code = id)).setAutoCancel(true).setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .addAction(NotificationCompat.Action.Builder(0, context.getString(R.string.reply), replyIntent).addRemoteInput(reply).build())
        if (code != null) n.setSubText(context.getString(R.string.has_code))
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        runCatching { NotificationManagerCompat.from(context).notify(id, n.build()) }
    }

    /** The AI proposes a message: nothing is sent until the user taps «Enviar». */
    fun proposal(context: Context, who: String, to: String, name: String, text: String): Boolean {
        if (!allowed(context)) return false
        channels(context)
        val id = (to + text).hashCode()
        val send = PendingIntent.getBroadcast(context, id, Intent(context, ActionReceiver::class.java).setAction(SEND).putExtra(TO, to).putExtra(TEXT, text).putExtra(ID, id),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val n = NotificationCompat.Builder(context, REQUESTS).setSmallIcon(R.drawable.ic_stat_sms)
            .setContentTitle(context.getString(R.string.proposal_title, who, name.ifBlank { to }))
            .setContentText(text).setStyle(NotificationCompat.BigTextStyle().bigText(text)).setAutoCancel(true)
            .setContentIntent(open(context, to, text, id))
            .addAction(0, context.getString(R.string.send), send)
            .addAction(0, context.getString(R.string.edit), open(context, to, text, id + 1)).build()
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return false
        return runCatching { NotificationManagerCompat.from(context).notify(id, n); true }.getOrDefault(false)
    }

    fun cancel(context: Context, id: Int) = NotificationManagerCompat.from(context).cancel(id)
}

/** Tells an open screen that the messages changed. */
object App {
    @Volatile var onChanged: () -> Unit = {}
    fun changed() = android.os.Handler(android.os.Looper.getMainLooper()).post { onChanged() }
}
