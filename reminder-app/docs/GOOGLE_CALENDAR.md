# Integración con Google Calendar

Cuando guardas un recordatorio, la app crea además un **evento en tu Google
Calendar** con su propio aviso (popup) usando la antelación que elegiste.

La integración **se habilita sola** cuando existe el archivo de credenciales; si
no está, la app funciona normal sin Calendar.

## 1. Configuración (una sola vez)

1. Entra a <https://console.cloud.google.com/> y crea un **proyecto**.
2. **APIs y servicios → Biblioteca** → busca **Google Calendar API** → *Habilitar*.
3. **Pantalla de consentimiento OAuth**:
   - Tipo de usuario: **Externo**.
   - Completa nombre de la app y tu correo.
   - En **Usuarios de prueba**, agrega tu propia cuenta de Google.
4. **Credenciales → Crear credenciales → ID de cliente de OAuth**:
   - Tipo de aplicación: **App de escritorio**.
   - Descarga el JSON.
5. Renombra el archivo descargado a **`client_secret.json`** y colócalo en:
   ```
   reminder-app/src/main/resources/client_secret.json
   ```
   > Este archivo y la carpeta `tokens/` están en `.gitignore`: **no se suben**.

## 2. Primer uso

- La primera vez que guardes un recordatorio se abrirá el **navegador** para que
  autorices el acceso a tu calendario.
- El token se guarda en `tokens/`, así que no se vuelve a pedir.
- A partir de ahí, cada recordatorio crea un evento en tu calendario `primary`.

## 3. Detalles técnicos

- Scope usado: `CALENDAR_EVENTS` (solo crea/gestiona eventos, no lee todo tu
  calendario).
- El evento dura 30 min por defecto y lleva un recordatorio `popup` con tus
  minutos de antelación.
- El login OAuth usa un servidor local temporal en el puerto `8888`.

## 4. Añadir otras integraciones

La arquitectura es enchufable. Para conectar otro servicio (Outlook, email,
webhook, Notion, etc.):

1. Implementa `com.reminder.app.service.integration.ReminderIntegration`.
2. Regístrala en `ControllerReminder.registerIntegrations()`.

El `IntegrationManager` las ejecuta en segundo plano y aísla errores por
integración, sin afectar a la app ni a las demás.
