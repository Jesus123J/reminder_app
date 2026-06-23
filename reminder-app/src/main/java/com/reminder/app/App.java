package com.reminder.app;

import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.fonts.roboto.FlatRobotoFont;
import com.reminder.app.config.AppConfig;
import com.reminder.app.config.Theme;
import com.reminder.app.controller.ControllerReminder;
import javax.swing.SwingUtilities;

/**
 * Punto de entrada de la aplicacion de recordatorios.
 *
 * Configura el Look and Feel (FlatLaf), asegura que el almacenamiento exista
 * y arranca el controlador principal dentro del Event Dispatch Thread (EDT),
 * tal como exige Swing para construir y manipular la interfaz de forma segura.
 */
public class App {

    public static void main(String[] args) {
        FlatRobotoFont.install();
        FlatLightLaf.setup();
        Theme.applyGlobalDefaults();
        AppConfig.createDocumentData();

        // Toda la UI de Swing debe construirse en el EDT.
        SwingUtilities.invokeLater(ControllerReminder::new);
    }
}
