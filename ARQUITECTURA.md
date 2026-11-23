# Arquitectura de SMS

Carpeta = package (`android/app/src/main/java/com/zirakusa/sms/`):

| Carpeta | Qué hay |
|---|---|
| `data/` | Lógica pura, con pruebas. `Messages` (`Box`, `Message`, `Thread`; `Sms`: conversaciones, búsqueda, `code` y `masked` para códigos de verificación, `parts` para contar SMS con el alfabeto GSM; `Numbers`); `Store` (`SmsBook`, `NoAccess`, `MemoryBook`, `Settings`, `KeyValues`). |
| `ui/` | `SmsState` (único dueño del estado: mensajes, conversación abierta, lo que se escribe y ajustes) y `SmsApp` (`AppActions`). |
| `ui/screens/` | `ThreadsScreen` (`Initials`, `whenText`), `ConversationScreen`, `NewScreen`, `SettingsScreen`, `McpScreen`, `Parts`, `Chrome` y `SettingsParts`. |
| `ui/theme/` | `Theme`: `ThemeMode` y `Accent` (azul de fábrica). |
| `mcp/` | Modo MCP: `Tokens` (`sms_…`), `SmsTools` (`sms_threads`, `sms_messages`, `sms_search`, `sms_unread`; con escritura `sms_send_request`; a través de `SmsData`), `McpHandler`, `RelayLink`, `McpMode` (`LiveData`) y `HubLink` (módulo `sms_`). |
| `AndroidBook.kt` | `Telephony.Sms` (leer, marcar leído si es predeterminada, guardar lo recibido y enviado como predeterminada), nombres por `PhoneLookup`, envío con `SmsManager`. |
| `Receivers.kt` | `SmsReceiver` (SMS_DELIVER / SMS_RECEIVED), `MmsReceiver`, `RespondService`, `SentReceiver`, `ActionReceiver` (Responder y Enviar). |
| `Notifier.kt` | Notificaciones de mensajes nuevos y de propuestas de la IA; `App` avisa a la pantalla. |
| `Summary.kt` | `SummaryLines` (pura) y `SummaryProvider`: no leídos para Inicio, con códigos ocultos. |
| `MainActivity.kt` | Crea el estado, pide permisos, ofrece ser predeterminada (RoleManager), abre `sms:`/`smsto:` y notificaciones. |
