package com.zirakusa.sms.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.sms.ui.theme.SmsColors
import com.zirakusa.sms.R
import com.zirakusa.sms.mcp.Access
import com.zirakusa.sms.mcp.Kind
import com.zirakusa.sms.mcp.McpMode
import com.zirakusa.sms.mcp.RelayLink
import com.zirakusa.sms.mcp.Token

/** Ajustes › Modo MCP, as in Stone, Luma, Calculadora and Notas, Filem y Nivel: Activar MCP · Redireccionar a Hub · Crear token (LLM o Hub) and the tokens. */
@Composable fun McpScreen(copy: (String) -> Unit, goToHub: () -> Unit, onBack: () -> Unit) {
    var creating by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(SmsColors.Background).systemBarsPadding().padding(16.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RoundButton(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.back), onClick = onBack)
            Text(stringResource(R.string.mcp_title), Modifier.padding(start = 16.dp), fontSize = 22.sp)
        }
        Card(stringResource(R.string.mcp_enable)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(if (McpMode.hubRedirect) R.string.mcp_enable_by_hub else R.string.mcp_enable_hint), Modifier.weight(1f), color = SmsColors.Muted)
                Switch(McpMode.active, McpMode::enable, enabled = !McpMode.hubRedirect, colors = accent())
            }
            if (McpMode.active) Text(stringResource(when (McpMode.state) {
                RelayLink.State.ONLINE -> R.string.mcp_online; RelayLink.State.CONNECTING -> R.string.mcp_connecting
                RelayLink.State.OFFLINE -> R.string.mcp_offline
            }), color = SmsColors.AccentText)
        }
        Card(stringResource(R.string.mcp_redirect)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.mcp_redirect_hint), Modifier.weight(1f), color = SmsColors.Muted)
                Switch(McpMode.hubRedirect, McpMode::redirect, colors = accent())
            }
            if (McpMode.hubRedirect) Button(goToHub, colors = ButtonDefaults.buttonColors(SmsColors.Accent, SmsColors.OnAccent)) { Text(stringResource(R.string.mcp_go_to_hub)) }
        }
        if (McpMode.hubRedirect) return@Column
        Card(stringResource(R.string.mcp_tokens)) {
            if (creating) Editor({ creating = false }) { copy(McpMode.linkFor(it.secret)); creating = false }
            else Button({ creating = true }, colors = ButtonDefaults.buttonColors(SmsColors.Accent, SmsColors.OnAccent)) { Text(stringResource(R.string.mcp_create)) }
            McpMode.listed.forEach { TokenRow(it, copy) }
        }
    }
}

@Composable private fun accent() = SwitchDefaults.colors(checkedTrackColor = SmsColors.Accent)

@Composable private fun Card(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.padding(top = 16.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(SmsColors.Card).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, fontSize = 18.sp)
        content()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable private fun Editor(onCancel: () -> Unit, onCreated: (Token) -> Unit) {
    var kind by remember { mutableStateOf(Kind.LLM) }
    var name by remember { mutableStateOf("") }
    var usage by remember { mutableStateOf("") }
    var access by remember { mutableStateOf(Access.READ) }
    var days by remember { mutableIntStateOf(0) }
    Text(stringResource(R.string.mcp_what), color = SmsColors.Muted)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Chip(kind == Kind.LLM, R.string.mcp_kind_llm) { kind = Kind.LLM }
        Chip(kind == Kind.HUB, R.string.mcp_kind_hub) { kind = Kind.HUB }
    }
    OutlinedTextField(name, { name = it.take(40) }, Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text(stringResource(R.string.mcp_name)) })
    OutlinedTextField(usage, { usage = it.take(Token.USAGE_MAX) }, Modifier.fillMaxWidth(), minLines = 2,
        placeholder = { Text(stringResource(R.string.mcp_usage)) }, supportingText = { Text("${usage.length} / ${Token.USAGE_MAX}") })
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Chip(access == Access.READ, R.string.mcp_read) { access = Access.READ }
        Chip(access == Access.WRITE, R.string.mcp_write) { access = Access.WRITE }
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(0, 7, 30, 90, 365).forEach { d ->
            FilterChip(days == d, { days = d }, { Text(if (d == 0) stringResource(R.string.mcp_forever) else stringResource(R.string.mcp_days, d)) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SmsColors.Accent, selectedLabelColor = SmsColors.OnAccent))
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        TextButton(onCancel) { Text(stringResource(R.string.cancel), color = SmsColors.Text) }
        Button({ onCreated(McpMode.create(name, access, kind, usage, days)) }, colors = ButtonDefaults.buttonColors(SmsColors.Accent, SmsColors.OnAccent)) {
            Text(stringResource(R.string.mcp_create_copy))
        }
    }
}

@Composable private fun Chip(on: Boolean, label: Int, onClick: () -> Unit) = FilterChip(on, onClick, { Text(stringResource(label)) },
    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SmsColors.Accent, selectedLabelColor = SmsColors.OnAccent))

@Composable private fun TokenRow(token: Token, copy: (String) -> Unit) {
    var confirm by remember { mutableStateOf(false) }
    HorizontalDivider(color = SmsColors.Chip)
    Text(token.name, fontSize = 17.sp)
    Text(listOf(stringResource(if (token.kind == Kind.HUB) R.string.mcp_kind_hub else R.string.mcp_kind_llm),
        stringResource(if (token.access == Access.WRITE) R.string.mcp_write else R.string.mcp_read),
        stringResource(when { token.paused -> R.string.mcp_paused; token.expired -> R.string.mcp_expired; else -> R.string.mcp_active })
    ).joinToString(" · "), color = SmsColors.Muted)
    if (token.usage.isNotBlank()) Text(token.usage, color = SmsColors.Muted, fontSize = 13.sp)
    Text(McpMode.linkFor(token.secret).replace(token.secret, token.secret.take(8) + "•••"), fontFamily = FontFamily.Monospace,
        fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = SmsColors.Muted)
    Row {
        TextButton({ copy(McpMode.linkFor(token.secret)) }) { Text(stringResource(R.string.mcp_copy), color = SmsColors.AccentText) }
        TextButton({ McpMode.setPaused(token.id, !token.paused) }) { Text(stringResource(if (token.paused) R.string.mcp_resume else R.string.mcp_pause), color = SmsColors.Text) }
        TextButton({ confirm = true }) { Text(stringResource(R.string.mcp_revoke), color = SmsColors.Danger) }
    }
    if (confirm) AlertDialog({ confirm = false }, title = { Text(stringResource(R.string.mcp_revoke_title, token.name)) },
        text = { Text(stringResource(R.string.mcp_revoke_text)) },
        confirmButton = { TextButton({ confirm = false; McpMode.revoke(token.id) }) { Text(stringResource(R.string.mcp_revoke), color = SmsColors.Danger) } },
        dismissButton = { TextButton({ confirm = false }) { Text(stringResource(R.string.cancel)) } })
}
