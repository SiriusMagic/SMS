# SMS (mensajes MCP)

Mensajes de texto para Android en negro con acento azul. Parte de la familia de Stone, Luma, Calculadora, Notas, Filem,
Brújula, Nivel, Reloj, Calendario, Inicio, QR, Clima, Redes, Contactos, Cámara y Teléfono. Por MCP tu IA lee tus mensajes
**sin códigos de verificación** y puede **proponerte** un mensaje; **nunca envía sola**.

## Estado (0.1.0)

- **Conversaciones:** búsqueda, no leídos en negrita con su número, nombre del contacto (099… y +593… son lo mismo).
- **Conversación:** burbujas (las tuyas a la derecha), «No enviado» en rojo, **«Copiar código»** en los códigos de
  verificación, y la caja para escribir con lo que queda y cuántos SMS ocupa (160 caracteres, o 70 con emojis y algunos
  acentos como «ú»).
- **Nuevo mensaje** por número; también se abre desde Contactos, Teléfono y cualquier enlace `sms:`/`smsto:`.
- **Notificaciones** de mensajes nuevos con **Responder** desde la notificación; texto visible u oculto (Ajustes).
- Funciona **sin ser la app de mensajes predeterminada** (Android guarda los SMS). Si la haces predeterminada (Ajustes),
  guarda también lo que llega y lo que envías. **MMS (fotos) aún no**: llegan con la 0.2.0.
- **Ajustes:** app predeterminada, texto en las notificaciones, códigos ocultos para la IA, **Modo MCP**, Temas, privacidad
  y versión.
- **Modo MCP:** `sms_threads`, `sms_messages`, `sms_search`, `sms_unread` y, con token de lectura y escritura,
  `sms_send_request`: una notificación «Claude propone enviar a Ana Pérez» con **Enviar** y **Editar**; nada sale hasta que
  tocas. Los códigos de verificación salen siempre como `••••••` (y buscarlos no encuentra nada), para que un token
  filtrado nunca abra tus cuentas. Tokens `sms_…`; módulo `sms_` para Gateway Hub.
- **Resumen para Inicio:** «Mensajes sin leer» (códigos ocultos) en la pantalla «Hoy».
- Sin anuncios ni estadísticas; internet solo para el Modo MCP (apagado de fábrica).

APK: [`android/SMS-0.1.0.apk`](android/SMS-0.1.0.apk) (`com.zirakusa.sms`, firmado como el resto de la familia y Gateway Hub).

## Compilar y probar

```bash
cd android
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleRelease
```

## Siguiente

Ver [`ROADMAP.md`](ROADMAP.md).
