package com.reminder.app.config;

import com.formdev.flatlaf.fonts.roboto.FlatRobotoFont;
import java.awt.Color;
import java.awt.Font;
import javax.swing.UIManager;

/**
 * Tema visual centralizado de la aplicacion.
 *
 * Define una paleta moderna (neutros "slate" + acento ambar) y tipografia
 * Roboto, evitando el problema de "letras negras" con bajo contraste. Aplica
 * ademas defaults globales sobre FlatLaf (esquinas redondeadas, fuente,
 * colores de tabla, etc.).
 *
 * @author Jesus Gutierrez
 */
public final class Theme {

    private Theme() {
        // Clase utilitaria.
    }

    // ----- Paleta (estilo moderno) -----
    /** Acento principal (ambar). */
    public static final Color PRIMARY = new Color(0xF5, 0x9E, 0x0B);
    /** Acento principal mas oscuro (hover / press). */
    public static final Color PRIMARY_DARK = new Color(0xD9, 0x77, 0x06);
    /** Color destructivo (eliminar). */
    public static final Color DANGER = new Color(0xEF, 0x44, 0x44);
    public static final Color DANGER_DARK = new Color(0xDC, 0x26, 0x26);

    /** Fondo general de la ventana. */
    public static final Color BACKGROUND = new Color(0xFF, 0xFF, 0xFF);
    /** Superficie de tarjetas / paneles (slate-100). */
    public static final Color SURFACE = new Color(0xF1, 0xF5, 0xF9);
    /** Borde/linea sutil (slate-200). */
    public static final Color LINE = new Color(0xE2, 0xE8, 0xF0);

    /** Texto principal (slate-800): oscuro y legible, no negro puro. */
    public static final Color TEXT = new Color(0x1E, 0x29, 0x3B);
    /** Texto secundario / placeholder (slate-500). */
    public static final Color TEXT_MUTED = new Color(0x64, 0x74, 0x8B);
    /** Texto sobre fondo de acento. */
    public static final Color TEXT_ON_PRIMARY = Color.WHITE;

    /** Fondo de fila seleccionada en la tabla (amber-100). */
    public static final Color SELECTION = new Color(0xFE, 0xF3, 0xC7);
    /** Filas alternas (zebra). */
    public static final Color ROW_EVEN = Color.WHITE;
    public static final Color ROW_ODD = new Color(0xF8, 0xFA, 0xFC);

    // ----- Tipografias -----
    private static final String FAMILY = FlatRobotoFont.FAMILY;

    public static Font fontRegular(float size) {
        return new Font(FAMILY, Font.PLAIN, (int) size);
    }

    public static Font fontBold(float size) {
        return new Font(FAMILY, Font.BOLD, (int) size);
    }

    /**
     * Aplica defaults globales coherentes sobre el Look and Feel ya inicializado.
     * Debe invocarse despues de {@code FlatLightLaf.setup()} y de instalar Roboto.
     */
    public static void applyGlobalDefaults() {
        // Tipografia global moderna.
        UIManager.put("defaultFont", fontRegular(13));

        // Esquinas redondeadas (look moderno).
        UIManager.put("Component.focusColor", PRIMARY);
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("Component.arc", 16);
        UIManager.put("Button.arc", 18);
        UIManager.put("TextComponent.arc", 14);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.thumbInsets", new java.awt.Insets(2, 2, 2, 2));

        UIManager.put("Table.showHorizontalLines", Boolean.TRUE);
        UIManager.put("Table.gridColor", LINE);
        UIManager.put("Table.selectionBackground", SELECTION);
        UIManager.put("Table.selectionForeground", TEXT);
        UIManager.put("Table.intercellSpacing", new java.awt.Dimension(0, 0));

        // Sin cambio de color al pasar/pulsar el mouse sobre el encabezado.
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
