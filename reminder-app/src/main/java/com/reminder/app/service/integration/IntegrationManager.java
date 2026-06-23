package com.reminder.app.service.integration;

import com.reminder.app.model.Reminder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Coordina todas las integraciones externas registradas.
 *
 * Mantiene la lista de {@link ReminderIntegration} y reenvia los eventos de
 * dominio (crear/eliminar recordatorio) a cada una. El despacho ocurre en un
 * pool de hilos de fondo para que las llamadas de red no congelen la UI, y los
 * errores de una integracion se registran sin afectar a las demas ni a la app.
 *
 * @author Jesus Gutierrez
 */
public class IntegrationManager {

    private static final Logger LOGGER = Logger.getLogger(IntegrationManager.class.getName());

    private final List<ReminderIntegration> integrations = new ArrayList<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "integration-dispatcher");
        t.setDaemon(true); // no impedir el cierre de la app
        return t;
    });

    /** Registra una integracion (se ignora si es null). */
    public void register(ReminderIntegration integration) {
        if (integration != null) {
            integrations.add(integration);
            LOGGER.log(Level.INFO, "Integracion registrada: {0} (enabled={1})",
                    new Object[]{integration.name(), integration.isEnabled()});
        }
    }

    public List<ReminderIntegration> getIntegrations() {
        return new ArrayList<>(integrations);
    }

    /** Reenvia a las integraciones habilitadas la creacion de un recordatorio. */
    public void dispatchCreated(Reminder reminder) {
        for (ReminderIntegration integration : integrations) {
            if (!integration.isEnabled()) {
                continue;
            }
            executor.submit(() -> {
                try {
                    integration.onReminderCreated(reminder);
                } catch (Exception ex) {
                    LOGGER.log(Level.WARNING,
                            "Fallo en integracion '" + integration.name() + "' (created)", ex);
                }
            });
        }
    }

    /** Reenvia a las integraciones habilitadas la eliminacion de un recordatorio. */
    public void dispatchDeleted(Reminder reminder) {
        for (ReminderIntegration integration : integrations) {
            if (!integration.isEnabled()) {
                continue;
            }
            executor.submit(() -> {
                try {
                    integration.onReminderDeleted(reminder);
                } catch (Exception ex) {
                    LOGGER.log(Level.WARNING,
                            "Fallo en integracion '" + integration.name() + "' (deleted)", ex);
                }
            });
        }
    }

    public void shutdown() {
        executor.shutdown();
    }
}
