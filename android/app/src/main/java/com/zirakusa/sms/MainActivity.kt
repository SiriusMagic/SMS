package com.zirakusa.sms

import android.Manifest
import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Telephony
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.SideEffect
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import com.zirakusa.sms.data.SharedValues
import com.zirakusa.sms.mcp.McpMode
import com.zirakusa.sms.ui.AppActions
import com.zirakusa.sms.ui.Page
import com.zirakusa.sms.ui.SmsApp
import com.zirakusa.sms.ui.SmsState
import com.zirakusa.sms.ui.theme.SmsColors
import com.zirakusa.sms.ui.theme.SmsTheme

/** Creates the state, asks for permissions, offers to be the default SMS app, and opens conversations from other apps and notifications. */
class MainActivity : ComponentActivity() {
    private lateinit var state: SmsState
    private lateinit var book: AndroidBook
    private val allow = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { state.load() }
    private val role = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { state.load() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        book = AndroidBook(this)
        state = SmsState(SharedValues(this), book) { book.isDefault }
        App.onChanged = { state.load() }
        McpMode.load(this)
        val actions = AppActions(allow = ::ask, makeDefault = ::makeDefault, call = { open(Intent(Intent.ACTION_DIAL, "tel:${android.net.Uri.encode(it)}".toUri())) },
            copy = { copy("SMS", it) }, copyLink = { copy("SMS MCP", it) }, goToHub = ::goToHub)
        state.load()
        if (!state.allowed) ask()
        handle(intent)
        setContent {
            SmsTheme(state.themeMode, state.accent) {
                val light = SmsColors.isLight
                SideEffect { WindowCompat.getInsetsController(window, window.decorView).run { isAppearanceLightStatusBars = light; isAppearanceLightNavigationBars = light } }
                BackHandler(state.page != Page.THREADS || state.query.isNotEmpty()) { if (state.page == Page.THREADS) state.query = "" else state.back() }
                SmsApp(state, actions)
            }
        }
    }

    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); handle(intent) }
    override fun onResume() { super.onResume(); state.load() }

    /** smsto:/sms: from other apps (Contactos, Teléfono…) and our own notifications open the conversation, with the text if any. */
    private fun handle(intent: Intent?) {
        intent ?: return
        val fromLink = intent.data?.takeIf { it.scheme == "smsto" || it.scheme == "sms" }?.schemeSpecificPart?.substringBefore('?')
        val to = fromLink ?: intent.getStringExtra(ADDRESS) ?: return
        val text = intent.getStringExtra(DRAFT) ?: intent.getStringExtra("sms_body") ?: intent.getStringExtra(Intent.EXTRA_TEXT)
        if (to.isNotBlank()) state.open(to, text)
    }

    private fun ask() = allow.launch(buildList {
        add(Manifest.permission.READ_SMS); add(Manifest.permission.SEND_SMS); add(Manifest.permission.RECEIVE_SMS); add(Manifest.permission.READ_CONTACTS)
        if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
    }.toTypedArray())

    /** Asks Android to make SMS the default messages app (the user confirms in Android's own dialog). */
    private fun makeDefault() {
        if (book.isDefault) return
        if (Build.VERSION.SDK_INT >= 29) getSystemService(RoleManager::class.java)?.takeIf { it.isRoleAvailable(RoleManager.ROLE_SMS) }
            ?.let { role.launch(it.createRequestRoleIntent(RoleManager.ROLE_SMS)) }
        else @Suppress("DEPRECATION") role.launch(Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT).putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName))
    }

    private fun open(intent: Intent) = try { startActivity(intent) } catch (_: ActivityNotFoundException) { Toast.makeText(this, R.string.no_app, Toast.LENGTH_SHORT).show() }

    private fun copy(label: String, text: String) {
        getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText(label, text))
        Toast.makeText(this, R.string.copied, Toast.LENGTH_SHORT).show()
    }

    /** «Ir al Hub»: copies the hub's link (in case it must be pasted) and opens the Gateway Hub app. */
    private fun goToHub() {
        copy("SMS › Gateway Hub", McpMode.hubLink())
        val hub = packageManager.getLaunchIntentForPackage("com.zirakusa.gatewayhub")
        if (hub != null) startActivity(hub) else Toast.makeText(this, R.string.mcp_paste_in_hub, Toast.LENGTH_LONG).show()
    }

    companion object { const val ADDRESS = "address"; const val DRAFT = "draft" }
}
