package com.reminder.app.service.integration;

import java.util.prefs.Preferences;

/**
 * Base para integraciones que el usuario puede activar/desactivar desde la UI.
 *
 * La preferencia (activada/desactivada) se guarda con {@link Preferences}, por
 * lo que se recuerda entre ejecuciones sin archivos ni configuracion manual.
 *
 * @author Jesus Gutierrez
 */
public abstract class ToggleableIntegration implements ReminderIntegration {

    private static final Preferences PREFS =
            Preferences.userRoot().node("com/reminder/app/integrations");

    @Override
    public boolean canToggle() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return PREFS.getBoolean(name(), defaultEnabled());
    }

    @Override
    public void setEnabled(boolean enabled) {
        PREFS.putBoolean(name(), enabled);
    }

    /** Estado por defecto la primera vez (desactivada salvo que se indique). */
    protected boolean defaultEnabled() {
        return false;
    }
}
