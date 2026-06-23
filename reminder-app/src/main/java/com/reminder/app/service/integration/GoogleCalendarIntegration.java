package com.reminder.app.service.integration;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.EventReminder;
import com.reminder.app.model.Reminder;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

/**
 * Integracion con Google Calendar.
 *
 * Al crear un recordatorio, inserta un evento en el calendario "primary" del
 * usuario con su propio aviso (popup) usando la antelacion configurada, de modo
 * que el evento tambien funcione como recordatorio dentro de Google.
 *
 * <p><b>Requisito (una sola vez):</b> el usuario debe crear unas credenciales
 * OAuth 2.0 de tipo "App de escritorio" en Google Cloud Console (con la API de
 * Calendar habilitada) y colocar el archivo descargado como
 * {@code src/main/resources/client_secret.json}. Si ese archivo no existe, la
 * integracion queda deshabilitada y la app funciona normal sin Calendar.</p>
 *
 * <p>La primera vez se abre el navegador para autorizar; el token se guarda en
 * la carpeta {@code tokens/} para no volver a pedirlo.</p>
 *
 * @author Jesus Gutierrez
 */
public class GoogleCalendarIntegration implements ReminderIntegration {

    private static final Logger LOGGER = Logger.getLogger(GoogleCalendarIntegration.class.getName());

    private static final String APPLICATION_NAME = "Reminder App";
    private static final String CREDENTIALS_RESOURCE = "/client_secret.json";
    private static final String TOKENS_DIRECTORY = "tokens";
    private static final int OAUTH_CALLBACK_PORT = 8888;
    private static final int DEFAULT_DURATION_MINUTES = 30;

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES =
            Collections.singletonList(CalendarScopes.CALENDAR_EVENTS);

    private final boolean credentialsPresent;
    private Calendar service; // se construye de forma perezosa tras el login

    public GoogleCalendarIntegration() {
        this.credentialsPresent = getClass().getResource(CREDENTIALS_RESOURCE) != null;
        if (!credentialsPresent) {
            LOGGER.info("Google Calendar deshabilitado: falta client_secret.json en resources");
        }
    }

    @Override
    public String name() {
        return "Google Calendar";
    }

    @Override
    public boolean isEnabled() {
        return credentialsPresent;
    }

    @Override
    public void onReminderCreated(Reminder reminder) throws Exception {
        Calendar svc = service();
        ZoneId zone = ZoneId.systemDefault();

        DateTime start = new DateTime(Date.from(reminder.dueAt().atZone(zone).toInstant()));
        DateTime end = new DateTime(Date.from(
                reminder.dueAt().plusMinutes(DEFAULT_DURATION_MINUTES).atZone(zone).toInstant()));

        EventReminder popup = new EventReminder()
                .setMethod("popup")
                .setMinutes(Math.max(0, reminder.getAdvanceMinutes()));

        Event event = new Event()
                .setSummary(reminder.getTitle())
                .setDescription(reminder.getDescription())
                .setStart(new EventDateTime().setDateTime(start).setTimeZone(zone.getId()))
                .setEnd(new EventDateTime().setDateTime(end).setTimeZone(zone.getId()))
                .setReminders(new Event.Reminders()
                        .setUseDefault(false)
                        .setOverrides(Collections.singletonList(popup)));

        Event created = svc.events().insert("primary", event).execute();
        LOGGER.info("Evento creado en Google Calendar: " + created.getHtmlLink());
    }

    /** Construye (una vez) el cliente de Calendar, ejecutando el login OAuth si hace falta. */
    private synchronized Calendar service() throws Exception {
        if (service == null) {
            NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            service = new Calendar.Builder(httpTransport, JSON_FACTORY, authorize(httpTransport))
                    .setApplicationName(APPLICATION_NAME)
                    .build();
        }
        return service;
    }

    private Credential authorize(NetHttpTransport httpTransport) throws Exception {
        try (InputStream in = getClass().getResourceAsStream(CREDENTIALS_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("No se encontro " + CREDENTIALS_RESOURCE);
            }
            GoogleClientSecrets clientSecrets =
                    GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                    httpTransport, JSON_FACTORY, clientSecrets, SCOPES)
                    .setDataStoreFactory(new FileDataStoreFactory(new File(TOKENS_DIRECTORY)))
                    .setAccessType("offline")
                    .build();

            LocalServerReceiver receiver = new LocalServerReceiver.Builder()
                    .setPort(OAUTH_CALLBACK_PORT)
                    .build();
            return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
        }
    }
}
