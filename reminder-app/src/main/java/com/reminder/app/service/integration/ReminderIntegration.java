package com.reminder.app.service.integration;

import com.reminder.app.model.Reminder;

/**
 * Punto de extension para integraciones externas (Google Calendar, Outlook,
 * email, webhooks, etc.).
 *
 * Para anadir una nueva conexion externa basta con implementar esta interfaz y
 * registrarla en el {@link IntegrationManager}. El manager invoca los metodos en
 * un hilo de fondo, por lo que las implementaciones pueden hacer llamadas de red
 * sin bloquear la interfaz.
 *
 * @author Jesus Gutierrez
 */
public interface ReminderIntegration {

    /** Nombre legible de la integracion (para logs y UI). */
    String name();

    /**
     * Indica si la integracion esta lista para usarse (p.ej. credenciales
     * presentes). Si devuelve false, el manager la omite.
     */
    boolean isEnabled();

    /** Se invoca cuando se crea un recordatorio. */
    void onReminderCreated(Reminder reminder) throws Exception;

    /** Se invoca cuando se elimina un recordatorio (por defecto, no hace nada). */
    default void onReminderDeleted(Reminder reminder) throws Exception {
        // Opcional: las integraciones que lo soporten pueden sobreescribirlo.
    }

    /**
     * Indica si el usuario puede activar/desactivar esta integracion desde la UI.
     * Las que dependen de configuracion externa (p.ej. credenciales) devuelven
     * false y se gestionan solas.
     */
    default boolean canToggle() {
        return false;
    }

    /** Activa o desactiva la integracion (solo si {@link #canToggle()}). */
    default void setEnabled(boolean enabled) {
        // Por defecto no hace nada.
    }

    /** Descripcion corta para mostrar en la UI de extensiones. */
    default String description() {
        return "";
    }
}
