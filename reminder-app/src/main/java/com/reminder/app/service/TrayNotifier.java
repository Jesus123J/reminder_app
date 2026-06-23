package com.reminder.app.service;

import java.awt.AWTException;
import java.awt.Image;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Notificador del sistema (bandeja de Windows).
 *
 * Muestra globos de notificacion nativos para que el aviso se vea aunque la
 * ventana no tenga el foco. Si la bandeja del sistema no esta disponible,
 * degrada silenciosamente (la app sigue usando los toasts internos).
 *
 * @author Jesus Gutierrez
 */
public class TrayNotifier {

    private static final Logger LOGGER = Logger.getLogger(TrayNotifier.class.getName());

    private TrayIcon trayIcon;

    public TrayNotifier() {
        if (!SystemTray.isSupported()) {
            LOGGER.info("SystemTray no soportado: se usaran solo los toasts internos");
            return;
        }
        try {
            Image image = loadIcon();
            trayIcon = new TrayIcon(image, "Recordatorios");
            trayIcon.setImageAutoSize(true);
            SystemTray.getSystemTray().add(trayIcon);
        } catch (AWTException ex) {
            LOGGER.log(Level.WARNING, "No se pudo registrar el icono de bandeja", ex);
            trayIcon = null;
        }
    }

    /** Muestra una notificacion nativa; no hace nada si la bandeja no esta disponible. */
    public void show(String title, String message) {
        if (trayIcon != null) {
            trayIcon.displayMessage(title, message, TrayIcon.MessageType.INFO);
        }
    }

    private Image loadIcon() {
        URL url = getClass().getResource("/icon-frame/noti.png");
        if (url != null) {
            return Toolkit.getDefaultToolkit().getImage(url);
        }
        // Imagen minima de respaldo si faltara el recurso.
        return Toolkit.getDefaultToolkit().createImage(new byte[0]);
    }
}
