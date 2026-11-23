# SMS (mensajes MCP): reglas para agentes

- Lee `ARQUITECTURA.md` (y el contrato común `FAMILIA.md` del repo de Inicio) y abre solo los archivos que necesites.
- Pruebas: `cd android && ./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleRelease` (debe pasar todo).
  Nunca desactives ni saltes una prueba. Lint sin avisos nuevos.
- Cada versión: sube `versionCode` y `versionName`, copia el APK a `android/SMS-<versión>.apk` (borra el anterior)
  y anótalo en `CHANGELOG.md`.
- Reglas puras (conversaciones, búsqueda, códigos de verificación, partes de SMS, números de Ecuador) en `data/`, con
  pruebas. Las pantallas solo pintan y avisan. Los mensajes pasan por `SmsBook` (`AndroidBook` / `MemoryBook`).
- Seguridad: la IA **nunca** envía; solo `sms_send_request` (notificación con «Enviar»). Todo texto que sale por MCP o a
  Inicio pasa por `Sms.masked`: los códigos de verificación jamás salen. Los receptores comprueban la acción; los de la app
  (`ActionReceiver`, `SentReceiver`) no se exportan.
- Privacidad: los SMS los guarda Android; la app no hace copias. Nada sale salvo por el Modo MCP que el usuario active;
  internet es solo para eso.
- Cada herramienta MCP nueva lleva su prueba en `McpTest`: lista de herramientas, llamada correcta, argumentos malos y
  «no encontrado». Las que cambian algo exigen token de lectura y escritura (`READ_ONLY` si no).
- Prohibido: `eval`, motores de scripts, SQL concatenado.
- Firma: la de depuración del entorno, igual que el resto de la familia y Gateway Hub.
