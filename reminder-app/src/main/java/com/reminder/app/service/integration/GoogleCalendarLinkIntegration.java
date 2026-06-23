package com.reminder.app.service.integration;

import com.reminder.app.model.Reminder;
import java.awt.Desktop;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;

/**
 * Anade el recordatorio a Google Calendar SIN credenciales del desarrollador.
 *
 * Abre en el navegador la pagina "crear evento" de Google con los datos ya
 * rellenados. Como el usuario ya tiene su sesion de Google abierta, solo pulsa
 * "Guardar": usa su propia cuenta y no requiere ningun token ni API key.
 *
 * Estandar abierto (URL template de Google Calendar), cero configuracion.
 *
 * @author Jesus Gutierrez
 */
public class GoogleCalendarLinkIntegration extends ToggleableIntegration {

    private static final Logger LOGGER =
            Logger.getLogger(GoogleCalendarLinkIntegration.class.getName());

    private static final DateTimeFormatter UTC_FMT =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");
    private static final int DEFAULT_DURATION_MINUTES = 30;

    @Override
    public String name() {
        return "Google Calendar (link)";
    }

    @Override
    public String description() {
        return "Abre el evento en tu cuenta de Google ya iniciada. Sin tokens.";
    }

    @Override
    public void onReminderCreated(Reminder reminder) throws Exception {
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            LOGGER.warning("El navegador no esta disponible para abrir Google Calendar");
            return;
        }
        String start = reminder.dueAt().atZone(ZoneOffset.systemDefault())
                .withZoneSameInstant(ZoneOffset.UTC).format(UTC_FMT);
        String end = reminder.dueAt().plusMinutes(DEFAULT_DURATION_MINUTES)
                .atZone(ZoneOffset.systemDefault())
                .withZoneSameInstant(ZoneOffset.UTC).format(UTC_FMT);

        String url = "https://calendar.google.com/calendar/render?action=TEMPLATE"
                + "&text=" + enc(reminder.getTitle())
                + "&dates=" + start + "/" + end
                + "&details=" + enc(reminder.getDescription());

        Desktop.getDesktop().browse(new URI(url));
        LOGGER.info("Abriendo Google Calendar para: " + reminder.getTitle());
    }

    private String enc(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}
