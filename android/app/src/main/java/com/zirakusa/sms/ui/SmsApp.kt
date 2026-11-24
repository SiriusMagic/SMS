package com.zirakusa.sms.ui

import androidx.compose.runtime.Composable
import com.zirakusa.sms.ui.screens.ConversationScreen
import com.zirakusa.sms.ui.screens.McpScreen
import com.zirakusa.sms.ui.screens.NewScreen
import com.zirakusa.sms.ui.screens.SettingsScreen
import com.zirakusa.sms.ui.screens.ThreadsScreen

/** What the screens ask of the phone (the activity gives the real ones; tests leave the defaults). */
class AppActions(
    val allow: () -> Unit = {},
    val makeDefault: () -> Unit = {},
    /** Android's page of this app (to allow restricted settings when the role was refused). */
    val settings: () -> Unit = {},
    val call: (String) -> Unit = {},
    val copy: (String) -> Unit = {},
    val copyLink: (String) -> Unit = {},
    val goToHub: () -> Unit = {},
)

@Composable fun SmsApp(state: SmsState, actions: AppActions = AppActions()) = when (state.page) {
    Page.THREADS -> ThreadsScreen(state, actions)
    Page.CONVERSATION -> ConversationScreen(state, actions)
    Page.NEW -> NewScreen(state)
    Page.SETTINGS -> SettingsScreen(state, actions)
    Page.MCP -> McpScreen(actions.copyLink, actions.goToHub) { state.page = Page.SETTINGS }
}
