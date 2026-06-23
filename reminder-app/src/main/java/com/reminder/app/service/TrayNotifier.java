package com.reminder.app.service;

import java.awt.AWTException;
import java.awt.Frame;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.WindowConstants;

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

    /**
     * Hace que al cerrar la ventana la app se minimice a la bandeja (y siga
     * avisando) en vez de salir. Anade un menu Abrir/Salir al icono. Si no hay
     * bandeja, mantiene el cierre normal (salir).
     */
    public void enableMinimizeToTray(JFrame frame) {
        if (trayIcon == null) {
            return; // sin bandeja: se conserva EXIT_ON_CLOSE
        }
        PopupMenu popup = new PopupMenu();
        MenuItem open = new MenuItem("Abrir");
        open.addActionListener(e -> restore(frame));
        MenuItem exit = new MenuItem("Salir");
        exit.addActionListener(e -> System.exit(0));
        popup.add(open);
        popup.addSeparator();
        popup.add(exit);
        trayIcon.setPopupMenu(popup);
        trayIcon.addActionListener(e -> restore(frame)); // doble clic en el icono

        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                frame.setVisible(false);
                show("Sigo activo", "Los recordatorios se siguen avisando en segundo plano.");
            }
        });
    }

    private void restore(JFrame frame) {
        frame.setVisible(true);
        frame.setState(Frame.NORMAL);
        frame.toFront();
        frame.requestFocus();
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
