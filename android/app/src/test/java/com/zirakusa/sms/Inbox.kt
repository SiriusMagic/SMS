package com.zirakusa.sms

import com.zirakusa.sms.data.Box
import com.zirakusa.sms.data.Message
import java.time.LocalDateTime
import java.time.ZoneOffset

/** The same messages for every test (UTC, 21 November 2026). */
object Inbox {
    fun at(h: Int, m: Int = 0) = LocalDateTime.of(2026, 11, 21, h, m).toInstant(ZoneOffset.UTC).toEpochMilli()
    val names = mapOf("0991234567" to "Ana Pérez")
    val all = listOf(
        Message(1, 10, "+593991234567", "Hola, ¿vamos mañana al cine?", at(9), Box.INBOX, read = false),
        Message(2, 10, "0991234567", "¡Sí! A las 7", at(9, 5), Box.SENT),
        Message(3, 10, "+593991234567", "Perfecto 🎬", at(9, 10), Box.INBOX, read = false),
        Message(4, 20, "BANCO", "Tu código de verificación es 482913. No lo compartas.", at(8), Box.INBOX, read = false),
        Message(5, 30, "0987654321", "Factura de 1250 dólares pagada el 2026-11-20", at(7), Box.INBOX),
        Message(6, 30, "0987654321", "Gracias", at(7, 30), Box.FAILED),
    )
}
