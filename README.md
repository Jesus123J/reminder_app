# 🔔 Reminder App (Java Desktop)

![Java](https://img.shields.io/badge/Java-17-blue?style=flat&logo=openjdk)
![Maven](https://img.shields.io/badge/Maven-3.9-C71A36?style=flat&logo=apachemaven)
![Swing](https://img.shields.io/badge/UI-Swing%20%2B%20FlatLaf-1E293B?style=flat)

Aplicación de **recordatorios de escritorio** para uso personal. Guarda tus
recordatorios **localmente** (privacidad total), te **avisa** cuando llega la
hora y, opcionalmente, los **registra en tu Google Calendar**.

---

## ✨ Funcionalidades

| Funcionalidad | Estado |
|---------------|:------:|
| 📝 Crear recordatorios (título, descripción, fecha, hora, antelación) | ✅ |
| 💾 Persistencia local en archivo (`data.txr`) | ✅ |
| 📋 Tabla con tus recordatorios cargados | ✅ |
| 🗑️ Eliminar por fila y **Eliminar todo** (con confirmación) | ✅ |
| ⏰ **Notificaciones programadas** (toast + bandeja de Windows) | ✅ |
| 📅 Integración con **Google Calendar** (evento + aviso) | ✅ |
| 🧩 Arquitectura **enchufable** para más integraciones | ✅ |
| 🎨 UI moderna (FlatLaf, fuente Roboto, paleta slate + ámbar) | ✅ |

---

## 🏗️ Arquitectura

Patrón **MVC** con capas de servicio y persistencia:

```
com.reminder.app
├── App                       → punto de entrada (EDT, FlatLaf, Roboto)
├── config
│   ├── AppConfig             → ruta del almacenamiento local
│   └── Theme                 → paleta, tipografía y defaults de UI
├── model
│   ├── Reminder              → entidad de dominio (dueAt / notifyAt)
│   └── ModelReminderData     → modelo de la vista
├── repository
│   └── ReminderRepository    → DAO: persistencia en data.txr (Base64 + '|')
├── service
│   ├── ReminderScheduler     → dispara avisos cuando vence el recordatorio
│   ├── TrayNotifier          → notificación nativa de la bandeja
│   └── integration
│       ├── ReminderIntegration       → interfaz punto-de-extensión
│       ├── IntegrationManager        → despacha a las integraciones (hilo bg)
│       └── GoogleCalendarIntegration → conexión con Google Calendar
├── controller
│   └── ControllerReminder    → coordina vista, repo, scheduler e integraciones
└── view
    ├── ViewReminder          → ventana principal (Swing)
    └── components / util     → componentes y estilos personalizados
```

### Persistencia
Cada recordatorio se guarda en una línea de `data.txr`:
```
id | base64(título) | base64(descripción) | fecha | hora | antelaciónMin | notificado
```
El texto libre va en **Base64** para tolerar cualquier carácter (pipes, saltos
de línea, acentos) sin romper el formato.

### Notificaciones
`ReminderScheduler` (usa `javax.swing.Timer`) revisa el repositorio cada 30 s y,
cuando `notifyAt` (vencimiento − antelación) ya pasó, muestra el aviso y marca
el recordatorio como notificado para no repetirlo.

---

## 🛠️ Tecnologías

- **Java 17** + **Maven**
- **Swing** con **FlatLaf** (look moderno) y fuente **Roboto**
- Selectores de fecha/hora `raven swing-datetime-picker`
- Toasts `raven swing-toast-notifications`
- **Google Calendar API** + OAuth 2.0 (integración opcional)

---

## 📦 Cómo ejecutar

### Requisitos
- JDK 17
- Maven 3.9+

### Pasos
```sh
git clone https://github.com/Jesus123J/reminder_app.git
cd reminder_app/reminder-app

# Compilar
mvn clean compile

# Ejecutar
mvn dependency:build-classpath -Dmdep.outputFile=cp.txt
java -cp "target/classes;$(cat cp.txt)" com.reminder.app.App
```
> En Windows con PowerShell usa `;` como separador de classpath (ya incluido arriba).

---

## 📅 Integración con Google Calendar

Cuando guardas un recordatorio, se crea además un **evento en tu Google
Calendar** con su propio aviso. La integración **se activa sola** al colocar tus
credenciales; sin ellas, la app funciona normal.

Pasos resumidos:
1. En **Google Cloud Console**: crea un proyecto y habilita **Google Calendar API**.
2. Configura la **pantalla de consentimiento OAuth** (Externo, agrégate como usuario de prueba).
3. Crea credenciales **OAuth → App de escritorio** y descarga el JSON.
4. Renómbralo a `client_secret.json` y ponlo en `reminder-app/src/main/resources/`.
5. La primera vez que guardes, el navegador te pedirá autorizar (una sola vez).

📄 Guía detallada: [`reminder-app/docs/GOOGLE_CALENDAR.md`](reminder-app/docs/GOOGLE_CALENDAR.md)

> `client_secret.json` y la carpeta `tokens/` están en `.gitignore`: **nunca se suben**.

---

## 🧩 Añadir otras integraciones

La arquitectura es enchufable. Para conectar otro servicio (Outlook, email,
webhook, Notion…):

1. Implementa `com.reminder.app.service.integration.ReminderIntegration`.
2. Regístrala en `ControllerReminder.registerIntegrations()`.

El `IntegrationManager` la ejecuta en segundo plano y aísla sus errores sin
afectar a la app ni a las demás integraciones.

---

## 🌿 Flujo de ramas

Cada cambio lógico va en su **propia rama** (`fix/*` o `feat/*`), se sube a
GitHub y luego se fusiona a `main`, que se mantiene siempre funcional.

---

## 👤 Autor
**Jesús Gutiérrez** — [@Jesus123J](https://github.com/Jesus123J)
