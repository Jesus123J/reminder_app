package com.reminder.app.service;

import java.io.File;
import java.util.prefs.Preferences;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.awt.Toolkit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Reproduce el sonido de la notificacion.
 *
 * Si el usuario configura un archivo .wav lo reproduce con
 * {@code javax.sound.sampled}; si no, usa el beep del sistema. Se puede silenciar.
 * Las preferencias se guardan con {@link Preferences} (sin archivos de config).
 *
 * @author Jesus Gutierrez
 */
public class SoundPlayer {

    private static final Logger LOGGER = Logger.getLogger(SoundPlayer.class.getName());
    private static final Preferences PREFS =
            Preferences.userRoot().node("com/reminder/app/sound");
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_FILE = "file";

    public boolean isEnabled() {
        return PREFS.getBoolean(KEY_ENABLED, true);
    }

    public void setEnabled(boolean enabled) {
        PREFS.putBoolean(KEY_ENABLED, enabled);
    }

    public String getSoundFile() {
        return PREFS.get(KEY_FILE, "");
    }

    public void setSoundFile(String path) {
        PREFS.put(KEY_FILE, path == null ? "" : path);
    }

    /** Reproduce el sonido configurado (o beep). No hace nada si esta silenciado. */
    public void play() {
        if (!isEnabled()) {
            return;
        }
        String file = getSoundFile();
        if (file != null && !file.isEmpty() && new File(file).isFile()) {
            playWav(new File(file));
        } else {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    private void playWav(File file) {
        try (AudioInputStream audio = AudioSystem.getAudioInputStream(file)) {
            Clip clip = AudioSystem.getClip();
            clip.open(audio);
            clip.start();
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "No se pudo reproducir el .wav, usando beep", ex);
            Toolkit.getDefaultToolkit().beep();
        }
    }
}
