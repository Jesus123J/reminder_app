package com.reminder.app.service.integration;

import com.reminder.app.model.Reminder;
import java.awt.Desktop;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;

/**
 * Exporta el recordatorio como archivo iCalendar (.ics) SIN credenciales.
 *
 * El estandar .ics lo entienden Google Calendar, Outlook y Apple Calendar: el
 * usuario solo abre/importa el archivo. Incluye una alarma (VALARM) con la
 * antelacion configurada, de modo que el evento tambien avise.
 *
 * Cero tokens, cero API: solo genera un archivo abierto y lo abre con la app de
 * calendario por defecto del sistema.
 *
 * @author Jesus Gutierrez
 */
public class IcsExportIntegration extends ToggleableIntegration {

    private static final Logger LOGGER = Logger.getLogger(IcsExportIntegration.class.getName());

    private static final DateTimeFormatter UTC_FMT =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");
    private static final String EXPORT_DIR = "calendar-export";
    private static final int DEFAULT_DURATION_MINUTES = 30;

    @Override
    public String name() {
        return "Exportar a calendario (.ics)";
    }

    @Override
    public String description() {
        return "Genera un .ics estandar para Google, Outlook o Apple. Sin tokens.";
    }

    @Override
    public void onReminderCreated(Reminder reminder) throws Exception {
        Path dir = Paths.get(EXPORT_DIR);
        Files.createDirectories(dir);
        Path file = dir.resolve("reminder-" + reminder.getId() + ".ics");
        Files.write(file, buildIcs(reminder).getBytes(StandardCharsets.UTF_8));
        LOGGER.info("ICS generado: " + file.toAbsolutePath());
        openIfPossible(file);
    }

    private String buildIcs(Reminder r) {
        String start = r.dueAt().atZone(ZoneOffset.systemDefault())
                .withZoneSameInstant(ZoneOffset.UTC).format(UTC_FMT);
        String end = r.dueAt().plusMinutes(DEFAULT_DURATION_MINUTES)
                .atZone(ZoneOffset.systemDefault())
                .withZoneSameInstant(ZoneOffset.UTC).format(UTC_FMT);

        return "BEGIN:VCALENDAR\r\n"
                + "VERSION:2.0\r\n"
                + "PRODID:-//Reminder App//ES\r\n"
                + "BEGIN:VEVENT\r\n"
                + "UID:" + r.getId() + "@reminder-app\r\n"
                + "DTSTAMP:" + start + "\r\n"
                + "DTSTART:" + start + "\r\n"
                + "DTEND:" + end + "\r\n"
                + "SUMMARY:" + esc(r.getTitle()) + "\r\n"
                + "DESCRIPTION:" + esc(r.getDescription()) + "\r\n"
                + "BEGIN:VALARM\r\n"
                + "ACTION:DISPLAY\r\n"
                + "DESCRIPTION:" + esc(r.getTitle()) + "\r\n"
                + "TRIGGER:-PT" + Math.max(0, r.getAdvanceMinutes()) + "M\r\n"
                + "END:VALARM\r\n"
                + "END:VEVENT\r\n"
                + "END:VCALENDAR\r\n";
    }

    /** Escapa los caracteres especiales del formato iCalendar. */
    private String esc(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace(";", "\\;")
                .replace(",", "\\,")
                .replace("\n", "\\n");
    }

    private void openIfPossible(Path file) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(file.toFile());
            }
        } catch (IOException ex) {
            // No es critico: el archivo queda generado aunque no se abra solo.
            LOGGER.info("ICS generado pero no se pudo abrir automaticamente: " + ex.getMessage());
        }
    }
}
