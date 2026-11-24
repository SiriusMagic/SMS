# SMS 0.1.2

- **«Permitir» ya deja pasar**: Android solo da el permiso de leer SMS a la app de mensajes del teléfono (a una app
  instalada a mano se lo niega en silencio, por eso la pantalla no avanzaba). Ahora «Permitir» pide primero que SMS sea tu
  app de mensajes y después los contactos y las notificaciones. Si dices que no, explica cómo hacerlo desde los ajustes
  («Permitir ajustes restringidos») con un botón que los abre.

# SMS 0.1.1

- **Conectar todas** (Gateway Hub 0.0.21): con un toque en el Hub se activa aquí «Redireccionar a Hub» y la app queda
  vinculada, sin entrar en ella. Solo lo puede pedir una app firmada por el mismo desarrollador.
- **Lucía** (la voz del Hub) usa las herramientas de esta app en el teléfono, **sin internet**, con las mismas reglas que
  por MCP. Solo mientras «Redireccionar a Hub» esté activado.
- **Icono de la app**: 5 iconos para elegir (Original, Claro, Dorado, Azul y Verde) en Ajustes › Icono de la app. Tras esta
  actualización quizá tengas que volver a poner el icono en tu pantalla de inicio (una sola vez).

# SMS 0.1.0

Primera versión: mensajes de texto en negro con acento azul.
- Conversaciones con búsqueda y no leídos; burbujas; copiar códigos; contador de caracteres y SMS.
- Nuevo mensaje y enlaces `sms:`/`smsto:`; notificaciones con Responder.
- Funciona sin ser predeterminada; opción para hacerla predeterminada. Sin MMS todavía.
- Modo MCP (`sms_`, módulo `sms_`): `sms_threads`, `sms_messages`, `sms_search`, `sms_unread`, `sms_send_request`
  (propuesta con «Enviar»). Códigos de verificación siempre ocultos.
- Resumen «Mensajes sin leer» para la pantalla «Hoy» de Inicio.
- 11 pruebas.
