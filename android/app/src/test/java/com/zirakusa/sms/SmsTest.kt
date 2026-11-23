package com.zirakusa.sms

import com.zirakusa.sms.data.MapValues
import com.zirakusa.sms.data.MemoryBook
import com.zirakusa.sms.data.Numbers
import com.zirakusa.sms.data.Sms
import com.zirakusa.sms.ui.Page
import com.zirakusa.sms.ui.SmsState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The pure rules (threads, search, codes, SMS parts, numbers) and the state: open, read, write, send. */
class SmsTest {
    private val book get() = MemoryBook(Inbox.all, Inbox.names)

    @Test fun threadsAndSearch() {
        val b = book
        val t = Sms.threads(Inbox.all, b::name)
        assertEquals(listOf(10L, 20L, 30L), t.map { it.id }); assertEquals("Ana Pérez", t[0].title); assertEquals(2, t[0].unread); assertEquals(3, t[0].count)
        assertEquals("BANCO", t[1].title)
        assertEquals(listOf(1L, 2L, 3L), Sms.conversation(Inbox.all, 10).map { it.id })
        assertEquals(listOf(1L), Sms.search(Inbox.all, "manana").map { it.id })        // accents ignored
        assertTrue(Sms.search(Inbox.all, "a").isEmpty())
    }

    @Test fun verificationCodes() {
        assertEquals("482913", Sms.code("Tu código de verificación es 482913. No lo compartas."))
        assertEquals("123456", Sms.code("Your verification code: 123-456"))
        assertEquals("5521", Sms.code("PIN 5521"))
        assertNull(Sms.code("Factura de 1250 dólares pagada el 2026-11-20"))            // numbers without a code word
        assertNull(Sms.code("Código postal: ninguno"))
        assertNull(Sms.code("Tu código de descuento vale 12.50 dólares"))
        assertEquals("7788", Sms.code("Code 7788, valid 5 min"))
        assertEquals("Tu código de verificación es ••••••. No lo compartas.", Sms.masked(Inbox.all[3].body))
        assertEquals("Your verification code: ••••••", Sms.masked("Your verification code: 123-456"))
        assertEquals(Inbox.all[4].body, Sms.masked(Inbox.all[4].body))
    }

    @Test fun partsAndNumbers() {
        assertEquals(1 to 160, Sms.parts("")); assertEquals(1 to 155, Sms.parts("Hola!")); assertEquals(1 to 0, Sms.parts("a".repeat(160)))
        assertEquals(2 to 145, Sms.parts("a".repeat(161)))                                // 153 per part
        assertEquals(1 to 158, Sms.parts("€"))                                            // € counts double
        assertEquals(1 to 68, Sms.parts("😀"))                                            // other characters: 70
        assertEquals(1 to 57, Sms.parts("¿Qué tal? ñ ú"))                                 // «ú» isn't in the basic alphabet: 70
        assertEquals(1 to 149, Sms.parts("¿Qué tal? ñ"))
        assertEquals("991234567", Numbers.key("+593 99 123 4567")); assertTrue(Numbers.valid("611")); assertFalse(Numbers.valid("12"))
        assertEquals("+593991234567", Numbers.clean(" +593 99-123 4567"))
    }

    @Test fun state() {
        val b = book
        val s = SmsState(MapValues(), b) { false }
        s.load()
        assertEquals(3, s.unread); assertFalse(s.defaultApp)
        s.open("0991234567")
        assertEquals(Page.CONVERSATION, s.page); assertEquals("Ana Pérez", s.title); assertEquals(3, s.conversation.size)  // 099… and +593… together
        assertEquals(1, s.unread)                                                           // opening reads it
        s.draft = "  "; assertFalse(s.send())
        s.draft = "Nos vemos"; assertTrue(s.send())
        assertEquals("", s.draft); assertEquals(listOf("0991234567" to "Nos vemos"), b.sent); assertEquals(4, s.conversation.size)
        s.back(); assertEquals(Page.THREADS, s.page)
        s.to = "12"; s.startNew(); assertEquals(Page.THREADS, s.page)
        s.to = "0999999999"; s.startNew(); assertEquals(Page.CONVERSATION, s.page); assertTrue(s.conversation.isEmpty())
        s.open("0987654321", "borrador"); assertEquals("borrador", s.draft)
        assertEquals(listOf("Ana Pérez (2) · Perfecto 🎬", "BANCO · Tu código de verificación es ••••••. No lo compartas."),
            SummaryLines.lines(Inbox.all, MemoryBook(Inbox.all, Inbox.names)::name))
        b.allowed = false; s.load(); assertFalse(s.allowed)
    }
}
