package com.reminder.app.service.integration;

import com.reminder.app.model.Reminder;
import java.awt.Desktop;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

/**
 * Abre un borrador de correo (mailto:) con el recordatorio, usando el cliente de
 * email por defecto del usuario. Sin credenciales ni servidor.
 *
 * @author Jesus Gutierrez
 */
public class EmailDraftIntegration extends ToggleableIntegration {

    private static final Logger LOGGER = Logger.getLogger(EmailDraftIntegration.class.getName());

    @Override
    public String name() {
        return "Email (borrador)";
    }

    @Override
    public String description() {
        return "Abre un borrador en tu cliente de correo. Sin tokens.";
    }

    @Override
    public void onReminderCreated(Reminder reminder) throws Exception {
        String subject = enc("Recordatorio: " + reminder.getTitle());
        String body = enc((reminder.getDescription() == null ? "" : reminder.getDescription())
                + "\n\nVence: " + reminder.dueAt());
        URI mailto = URI.create("mailto:?subject=" + subject + "&body=" + body);

        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.MAIL)) {
            Desktop.getDesktop().mail(mailto);
        } else if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(mailto);
        } else {
            LOGGER.warning("No hay cliente de correo disponible");
        }
    }

    /** mailto usa %20 para espacios (no '+'). */
    private String enc(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
