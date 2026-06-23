package com.reminder.app.service.integration;

import com.reminder.app.model.Reminder;
import java.awt.Desktop;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;

/**
 * Anade el recordatorio a Outlook Calendar SIN credenciales del desarrollador,
 * abriendo el deeplink de "componer evento" en la sesion del usuario.
 *
 * @author Jesus Gutierrez
 */
public class OutlookCalendarLinkIntegration extends ToggleableIntegration {

    private static final Logger LOGGER =
            Logger.getLogger(OutlookCalendarLinkIntegration.class.getName());
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final int DEFAULT_DURATION_MINUTES = 30;

    @Override
    public String name() {
        return "Outlook Calendar (link)";
    }

    @Override
    public String description() {
        return "Abre el evento en tu Outlook ya iniciado. Sin tokens.";
    }

    @Override
    public void onReminderCreated(Reminder reminder) throws Exception {
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            return;
        }
        String start = reminder.dueAt().format(ISO);
        String end = reminder.dueAt().plusMinutes(DEFAULT_DURATION_MINUTES).format(ISO);
        String url = "https://outlook.live.com/calendar/0/deeplink/compose?path=/calendar/action/compose"
                + "&subject=" + enc(reminder.getTitle())
                + "&startdt=" + start
                + "&enddt=" + end
                + "&body=" + enc(reminder.getDescription());
        Desktop.getDesktop().browse(new URI(url));
        LOGGER.info("Abriendo Outlook Calendar para: " + reminder.getTitle());
    }

    private String enc(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}
