package com.reminder.app.service.integration;

import com.reminder.app.model.Reminder;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Envia el recordatorio como POST JSON a un webhook configurable (Zapier, Make,
 * Discord, n8n, tu propio servidor...). Sin credenciales del desarrollador.
 *
 * La URL se toma de la variable de entorno {@code REMINDER_WEBHOOK_URL}. Si no
 * esta definida, la integracion no envia nada (solo lo registra).
 *
 * @author Jesus Gutierrez
 */
public class WebhookIntegration extends ToggleableIntegration {

    private static final Logger LOGGER = Logger.getLogger(WebhookIntegration.class.getName());
    private static final String ENV_URL = "REMINDER_WEBHOOK_URL";

    private final HttpClient http = HttpClient.newHttpClient();

    @Override
    public String name() {
        return "Webhook (POST JSON)";
    }

    @Override
    public String description() {
        return "POST JSON a la URL de REMINDER_WEBHOOK_URL (Zapier, Discord, etc.).";
    }

    @Override
    public void onReminderCreated(Reminder reminder) throws Exception {
        String url = System.getenv(ENV_URL);
        if (url == null || url.isBlank()) {
            LOGGER.info("Webhook activo pero sin REMINDER_WEBHOOK_URL definida; no se envia");
            return;
        }
        String json = "{"
                + "\"title\":" + jsonStr(reminder.getTitle()) + ","
                + "\"description\":" + jsonStr(reminder.getDescription()) + ","
                + "\"dueAt\":" + jsonStr(reminder.dueAt().toString())
                + "}";
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        LOGGER.log(Level.INFO, "Webhook respondio: {0}", response.statusCode());
    }

    private String jsonStr(String value) {
        if (value == null) {
            return "\"\"";
        }
        String escaped = value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "");
        return "\"" + escaped + "\"";
    }
}
