package com.zirakusa.sms

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.zirakusa.sms.data.MapValues
import com.zirakusa.sms.data.MemoryBook
import com.zirakusa.sms.ui.Page
import com.zirakusa.sms.ui.SmsApp
import com.zirakusa.sms.ui.SmsState
import com.zirakusa.sms.ui.theme.SmsTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Conversations, one conversation (with a code to copy and the SMS counter), new message, Ajustes and the permission card. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "es-w411dp-h891dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScreenshotsTest {
    @get:Rule val compose = createComposeRule()
    private fun shot(name: String) { compose.waitForIdle(); onView(isRoot()).captureRoboImage("build/screenshots/sms-$name.png") }

    @Test fun allScreens() {
        val book = MemoryBook(Inbox.all, Inbox.names)
        val state = SmsState(MapValues(), book) { false }
        state.load()
        compose.setContent { SmsTheme(state.themeMode, state.accent) { SmsApp(state) } }
        compose.onNodeWithText("Ana Pérez").assertExists()
        shot("threads")
        compose.onNodeWithText("BANCO").performClick()
        compose.onNodeWithText("Copiar código 482913").assertExists()
        state.open("0991234567")
        state.draft = "Nos vemos a las 7 en la entrada del cine"
        compose.onNodeWithText("Perfecto 🎬").assertExists()
        shot("conversation")
        state.page = Page.NEW; state.to = "0999999999"
        compose.onNodeWithText("Siguiente").assertExists()
        state.page = Page.SETTINGS
        compose.onNodeWithText("Códigos ocultos para la IA").assertExists()
        shot("settings")
        book.allowed = false; state.load(); state.page = Page.THREADS
        compose.onNodeWithText("Permitir").assertExists()
    }
}
