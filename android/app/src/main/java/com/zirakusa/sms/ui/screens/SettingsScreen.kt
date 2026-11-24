package com.zirakusa.sms.ui.screens

import com.zirakusa.sms.IconCard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zirakusa.sms.BuildConfig
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.unit.sp
import com.zirakusa.sms.R
import com.zirakusa.sms.mcp.McpMode
import com.zirakusa.sms.mcp.RelayLink
import com.zirakusa.sms.ui.Page
import com.zirakusa.sms.ui.SmsState
import com.zirakusa.sms.ui.theme.SmsColors

/** «Ajustes»: default app, notification preview, codes, Modo MCP, Temas, privacy and version. */
@Composable fun SettingsScreen(state: SmsState, actions: com.zirakusa.sms.ui.AppActions) {
    var privacy by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(SmsColors.Background).systemBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PageTop(stringResource(R.string.settings)) { state.page = Page.THREADS }
        Group {
            Item(stringResource(R.string.default_app), stringResource(if (state.defaultApp) R.string.default_yes else R.string.default_no), onClick = actions.makeDefault)
            Divider()
            SwitchItem(stringResource(R.string.preview), stringResource(R.string.preview_hint), state.preview, onChange = state::changePreview)
            Divider()
            Item(stringResource(R.string.codes_safe), stringResource(R.string.codes_safe_hint), onClick = null)
        }
        Group {
            val status = when {
                !McpMode.active -> stringResource(R.string.mcp_status_off)
                McpMode.hubRedirect -> stringResource(R.string.mcp_redirect)
                McpMode.state == RelayLink.State.ONLINE -> stringResource(R.string.mcp_online)
                McpMode.state == RelayLink.State.CONNECTING -> stringResource(R.string.mcp_connecting)
                else -> stringResource(R.string.mcp_offline)
            }
            Item(stringResource(R.string.mcp_title), "$status · ${stringResource(R.string.mcp_settings_hint)}") { state.page = Page.MCP }
        }
        IconCard(SmsColors.Card, SmsColors.Text, SmsColors.Muted, SmsColors.Accent, androidx.compose.ui.graphics.Color(0xFF0A1420))
        ThemeCard(state.themeMode, state.accent, state::changeTheme, state::changeAccent)
        Caption(stringResource(R.string.about))
        Group {
            Item(stringResource(R.string.privacy), null) { privacy = true }
            Divider()
            Item(stringResource(R.string.version_title), BuildConfig.VERSION_NAME, onClick = null)
        }
        Text("", Modifier.padding(bottom = 16.dp))
    }
    if (privacy) AlertDialog({ privacy = false }, confirmButton = { TextButton({ privacy = false }) { Text(stringResource(R.string.done), color = SmsColors.AccentText) } },
        title = { Text(stringResource(R.string.privacy)) }, text = { Text(stringResource(R.string.privacy_text)) })
}
