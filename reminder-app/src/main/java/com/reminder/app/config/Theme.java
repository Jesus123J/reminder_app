package com.reminder.app.config;

import java.awt.Color;
import java.awt.Font;
import javax.swing.UIManager;

/**
 * Tema visual centralizado de la aplicacion.
 *
 * Define una paleta coherente y tipografias legibles para evitar el problema
 * de "letras negras" con bajo contraste y los colores repartidos por toda la UI.
 * Tambien aplica valores por defecto globales sobre FlatLaf.
 *
 * @author Jesus Gutierrez
 */
public final class Theme {

    private Theme() {
        // Clase utilitaria.
    }

    // ----- Paleta -----
    /** Acento principal (ambar). */
    public static final Color PRIMARY = new Color(0xFF, 0x9F, 0x1C);
    /** Acento principal mas oscuro (hover / press). */
    public static final Color PRIMARY_DARK = new Color(0xF5, 0x7C, 0x00);
    /** Color destructivo (eliminar). */
    public static final Color DANGER = new Color(0xE5, 0x3E, 0x3E);

    /** Fondo general de la ventana. */
    public static final Color BACKGROUND = new Color(0xFF, 0xFF, 0xFF);
    /** Superficie de tarjetas / paneles. */
    public static final Color SURFACE = new Color(0xF4, 0xF6, 0xFA);
    /** Borde/linea sutil. */
    public static final Color LINE = new Color(0xD7, 0xDC, 0xE3);

    /** Texto principal: gris muy oscuro, mas legible que el negro puro. */
    public static final Color TEXT = new Color(0x33, 0x37, 0x3D);
    /** Texto secundario / placeholder. */
    public static final Color TEXT_MUTED = new Color(0x8A, 0x8F, 0x98);
    /** Texto sobre fondo de acento. */
    public static final Color TEXT_ON_PRIMARY = Color.WHITE;

    /** Fondo de fila seleccionada en la tabla. */
    public static final Color SELECTION = new Color(0xFF, 0xE0, 0xB2);
    /** Filas alternas (zebra). */
    public static final Color ROW_EVEN = Color.WHITE;
    public static final Color ROW_ODD = new Color(0xFB, 0xFC, 0xFE);

    // ----- Tipografias -----
    private static final String FAMILY = Font.SANS_SERIF;

    public static Font fontRegular(float size) {
        return new Font(FAMILY, Font.PLAIN, (int) size);
    }

    public static Font fontBold(float size) {
        return new Font(FAMILY, Font.BOLD, (int) size);
    }

    /**
     * Aplica defaults globales coherentes sobre el Look and Feel ya inicializado.
     * Debe invocarse despues de {@code FlatLightLaf.setup()}.
     */
    public static void applyGlobalDefaults() {
        // Acento de FlatLaf (focus, seleccion, etc.).
        UIManager.put("Component.focusColor", PRIMARY);
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("Component.arc", 12);
        UIManager.put("Button.arc", 14);
        UIManager.put("TextComponent.arc", 10);

        UIManager.put("Table.showHorizontalLines", Boolean.TRUE);
        UIManager.put("Table.gridColor", LINE);
        UIManager.put("Table.selectionBackground", SELECTION);
        UIManager.put("Table.selectionForeground", TEXT);

        // Sin cambio de color al pasar/pulsar el mouse sobre el encabezado:
        // se fuerza el mismo ambar/blanco en todos los estados para que el hover
        // no se note y se mantenga el ambar plano y limpio.
        UIManager.put("TableHeader.hoverBackground", PRIMARY);
        UIManager.put("TableHeader.pressedBackground", PRIMARY);
        UIManager.put("TableHeader.hoverForeground", TEXT_ON_PRIMARY);
        UIManager.put("TableHeader.pressedForeground", TEXT_ON_PRIMARY);
        UIManager.put("TableHeader.showTrailingVerticalLine", Boolean.FALSE);

        UIManager.put("Label.foreground", TEXT);
        UIManager.put("TextField.foreground", TEXT);
        UIManager.put("TextArea.foreground", TEXT);
        UIManager.put("ComboBox.foreground", TEXT);
    }
}
