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
        // En un hilo daemon para no bloquear el EDT mientras suena.
        Thread t = new Thread(() -> {
            String file = getSoundFile();
            if (file != null && !file.isEmpty() && new File(file).isFile()) {
                playWav(new File(file));
            } else {
                playTone();
            }
        }, "sound-player");
        t.setDaemon(true);
        t.start();
    }

    private void playWav(File file) {
        try (AudioInputStream audio = AudioSystem.getAudioInputStream(file)) {
            Clip clip = AudioSystem.getClip();
            clip.open(audio);
            clip.start();
            // Espera a que termine para que el Clip no se cierre antes de sonar.
            clip.drain();
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "No se pudo reproducir el .wav, usando tono", ex);
            playTone();
        }
    }

    /**
     * Genera y reproduce un tono audible (dos notas, tipo "ding-dong") con
     * javax.sound.sampled. No depende del "sonido de evento" de Windows, asi que
     * suena aunque el beep del sistema este desactivado.
     */
    private void playTone() {
        try {
            float sampleRate = 44100f;
            javax.sound.sampled.AudioFormat format =
                    new javax.sound.sampled.AudioFormat(sampleRate, 16, 1, true, false);
            javax.sound.sampled.SourceDataLine line = AudioSystem.getSourceDataLine(format);
            line.open(format);
            line.start();
            writeNote(line, sampleRate, 880, 180);  // La5
            writeNote(line, sampleRate, 1175, 220);  // Re6
            line.drain();
            line.stop();
            line.close();
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "No se pudo reproducir el tono, usando beep", ex);
            Toolkit.getDefaultToolkit().beep();
        }
    }

    private void writeNote(javax.sound.sampled.SourceDataLine line, float sampleRate,
                           double freq, int millis) {
        int samples = (int) (millis * sampleRate / 1000);
        byte[] buffer = new byte[samples * 2];
        for (int i = 0; i < samples; i++) {
            // Envolvente para evitar clicks (fade in/out).
            double env = Math.min(1.0, Math.min(i, samples - i) / (sampleRate * 0.02));
            double angle = 2.0 * Math.PI * i * freq / sampleRate;
            short val = (short) (Math.sin(angle) * env * 18000);
            buffer[i * 2] = (byte) (val & 0xFF);
            buffer[i * 2 + 1] = (byte) ((val >> 8) & 0xFF);
        }
        line.write(buffer, 0, buffer.length);
    }
}
