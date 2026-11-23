package com.zirakusa.sms.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.sms.R
import com.zirakusa.sms.data.Box as MailBox
import com.zirakusa.sms.data.Message
import com.zirakusa.sms.data.Sms
import com.zirakusa.sms.ui.AppActions
import com.zirakusa.sms.ui.SmsState
import com.zirakusa.sms.ui.theme.SmsColors

/** One conversation: bubbles (yours on the right), «Copiar código» on verification codes, and the box to write with its SMS count. */
@Composable fun ConversationScreen(state: SmsState, actions: AppActions) = Column(Modifier.fillMaxSize().background(SmsColors.Background).statusBarsPadding()
    .navigationBarsPadding().imePadding()) {
    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        RoundButton(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.back), onClick = state::back)
        Initials(state.title, 40.dp)
        Text(state.title, Modifier.weight(1f).padding(start = 12.dp), fontSize = 19.sp, maxLines = 1)
        RoundButton(Icons.Outlined.Call, stringResource(R.string.call)) { actions.call(state.address) }
    }
    val list = state.conversation
    val scroll = rememberLazyListState()
    LaunchedEffect(list.size) { if (list.isNotEmpty()) scroll.scrollToItem(list.lastIndex) }
    LazyColumn(Modifier.weight(1f), state = scroll, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items(list, key = { it.id }) { Bubble(it, actions) }
    }
    if (state.failedToSend) Text(stringResource(R.string.send_failed), Modifier.padding(horizontal = 16.dp), color = SmsColors.Danger, fontSize = 13.sp)
    Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.Bottom) {
        val (parts, left) = state.parts
        OutlinedTextField(state.draft, { state.draft = it.take(1600) }, Modifier.weight(1f), shape = RoundedCornerShape(24.dp), maxLines = 5,
            placeholder = { Text(stringResource(R.string.write)) },
            supportingText = { if (state.draft.isNotEmpty()) Text(if (parts > 1) pluralStringResource(R.plurals.count_parts, parts, left, parts) else stringResource(R.string.count, left)) })
        Box(Modifier.padding(start = 8.dp, bottom = 22.dp).size(52.dp).clip(CircleShape).background(if (state.draft.isBlank()) SmsColors.Chip else SmsColors.Accent)
            .clickable(enabled = state.draft.isNotBlank(), onClickLabel = stringResource(R.string.send)) { state.send() }, contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Outlined.Send, stringResource(R.string.send), tint = if (state.draft.isBlank()) SmsColors.Muted else SmsColors.OnAccent)
        }
    }
}

@Composable private fun Bubble(m: Message, actions: AppActions) = Box(Modifier.fillMaxWidth(), contentAlignment = if (m.mine) Alignment.CenterEnd else Alignment.CenterStart) {
    Column(Modifier.widthIn(max = 300.dp).clip(RoundedCornerShape(20.dp)).background(if (m.mine) SmsColors.Accent else SmsColors.Card).padding(horizontal = 14.dp, vertical = 9.dp)) {
        Text(m.body, color = if (m.mine) SmsColors.OnAccent else SmsColors.Text, fontSize = 16.sp)
        Sms.code(m.body)?.let { code ->
            Row(Modifier.padding(top = 6.dp).clip(CircleShape).background(SmsColors.Background.copy(alpha = 0.35f)).clickable { actions.copy(code) }
                .padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.ContentCopy, null, Modifier.size(16.dp), tint = if (m.mine) SmsColors.OnAccent else SmsColors.AccentText)
                Text(stringResource(R.string.copy_code, code), Modifier.padding(start = 6.dp), color = if (m.mine) SmsColors.OnAccent else SmsColors.AccentText, fontSize = 13.sp)
            }
        }
        Text(whenText(m.at) + if (m.box == MailBox.FAILED) " · " + stringResource(R.string.not_sent) else "", Modifier.padding(top = 2.dp),
            color = if (m.box == MailBox.FAILED) SmsColors.Danger else (if (m.mine) SmsColors.OnAccent else SmsColors.Muted).copy(alpha = 0.7f), fontSize = 11.sp)
    }
}
