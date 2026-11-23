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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.sms.R
import com.zirakusa.sms.data.Thread
import com.zirakusa.sms.ui.AppActions
import com.zirakusa.sms.ui.Page
import com.zirakusa.sms.ui.SmsState
import com.zirakusa.sms.ui.theme.SmsColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** «Mensajes»: search, then each conversation (unread in bold with a count); the pencil writes a new one. */
@Composable fun ThreadsScreen(state: SmsState, actions: AppActions) = Box(Modifier.fillMaxSize().background(SmsColors.Background)) {
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp)) {
        Row(Modifier.padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.messages), Modifier.weight(1f).padding(start = 4.dp), fontSize = 26.sp)
            RoundButton(Icons.Outlined.Settings, stringResource(R.string.settings)) { state.page = Page.SETTINGS }
        }
        if (!state.allowed) return@Column Group { Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Outlined.Sms, null, Modifier.size(48.dp), tint = SmsColors.AccentText)
            Text(stringResource(R.string.allow_text), color = SmsColors.Muted, textAlign = TextAlign.Center)
            MainButton(stringResource(R.string.allow), onClick = actions.allow)
        } }
        OutlinedTextField(state.query, { state.query = it.take(100) }, Modifier.fillMaxWidth(), singleLine = true, shape = CircleShape,
            leadingIcon = { Icon(Icons.Outlined.Search, null) }, placeholder = { Text(stringResource(R.string.search)) })
        if (!state.defaultApp) Text(stringResource(R.string.not_default), Modifier.fillMaxWidth().padding(top = 10.dp).clip(RoundedCornerShape(16.dp))
            .background(SmsColors.Card).clickable(onClick = actions.makeDefault).padding(14.dp), color = SmsColors.Muted, fontSize = 14.sp)
        LazyColumn(contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp)) {
            if (state.query.isNotBlank()) {
                val found = state.found
                if (found.isEmpty()) item { Text(stringResource(R.string.nothing_found), Modifier.padding(16.dp), color = SmsColors.Muted) }
                items(found, key = { it.id }) { m -> Line(m.address, m.body, m.at, 0) { state.open(m.address) } }
            } else {
                val threads = state.threads
                if (threads.isEmpty()) item { Text(stringResource(R.string.no_messages), Modifier.padding(16.dp), color = SmsColors.Muted) }
                items(threads, key = { it.id }) { t: Thread -> Line(t.title, (if (t.last.mine) stringResource(R.string.you) + " " else "") + t.last.body, t.last.at, t.unread) { state.open(t.address) } }
            }
        }
    }
    if (state.allowed) Box(Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(20.dp).size(60.dp).clip(RoundedCornerShape(20.dp)).background(SmsColors.Accent)
        .clickable(onClickLabel = stringResource(R.string.new_message)) { state.page = Page.NEW }, contentAlignment = Alignment.Center) {
        Icon(Icons.Outlined.Edit, stringResource(R.string.new_message), tint = SmsColors.OnAccent)
    }
}

@Composable private fun Line(who: String, text: String, at: Long, unread: Int, onClick: () -> Unit) =
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick).padding(vertical = 10.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Initials(who)
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(who, Modifier.weight(1f), fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = if (unread > 0) FontWeight.Bold else FontWeight.Normal)
                Text(whenText(at), color = if (unread > 0) SmsColors.AccentText else SmsColors.Muted, fontSize = 12.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text.replace('\n', ' '), Modifier.weight(1f), color = if (unread > 0) SmsColors.Text else SmsColors.Muted, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (unread > 0) Text("$unread", Modifier.padding(start = 8.dp).clip(CircleShape).background(SmsColors.Accent).padding(horizontal = 7.dp, vertical = 1.dp),
                    color = SmsColors.OnAccent, fontSize = 12.sp)
            }
        }
    }

/** A round picture with the initials (or # for numbers). */
@Composable fun Initials(who: String, size: androidx.compose.ui.unit.Dp = 44.dp) = Box(Modifier.size(size).clip(CircleShape).background(SmsColors.Accent.copy(alpha = 0.2f)),
    contentAlignment = Alignment.Center) {
    val letters = who.split(' ').filter { it.firstOrNull()?.isLetter() == true }.take(2).joinToString("") { it.take(1).uppercase() }.ifEmpty { "#" }
    Text(letters, color = SmsColors.AccentText, fontSize = (size.value * 0.38f).sp, fontWeight = FontWeight.SemiBold)
}

/** «9:05» today, «ayer»-style short date otherwise. */
@Composable fun whenText(at: Long, zone: ZoneId = ZoneId.systemDefault()): String {
    val t = Instant.ofEpochMilli(at).atZone(zone)
    return if (t.toLocalDate() == LocalDate.now(zone)) DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(t) else DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).format(t)
}

