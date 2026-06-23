package com.reminder.app.config;

import com.formdev.flatlaf.fonts.roboto.FlatRobotoFont;
import java.awt.Color;
import java.awt.Font;
import java.util.prefs.Preferences;
import javax.swing.UIManager;

/**
 * Tema visual centralizado de la aplicacion (claro/oscuro).
 *
 * Define una paleta moderna y tipografia Roboto. Soporta dos modos (claro y
 * oscuro) cuyas constantes de color se recalculan con {@link #applyMode(boolean)};
 * la preferencia se guarda con {@link Preferences}.
 *
 * @author Jesus Gutierrez
 */
public final class Theme {

    private Theme() {
    }

    private static final Preferences PREFS = Preferences.userRoot().node("com/reminder/app/theme");
    private static final String KEY_DARK = "dark";

    /** Modo actual. */
    public static boolean dark;

    // ----- Paleta (se reasigna segun el modo) -----
    public static Color PRIMARY;
    public static Color PRIMARY_DARK;
    public static Color DANGER;
    public static Color DANGER_DARK;
    public static Color BACKGROUND;
    public static Color SURFACE;
    public static Color LINE;
    public static Color TEXT;
    public static Color TEXT_MUTED;
    public static Color TEXT_ON_PRIMARY;
    public static Color SELECTION;
    public static Color ROW_EVEN;
    public static Color ROW_ODD;

    static {
        applyMode(isDarkPreferred());
    }

    public static boolean isDarkPreferred() {
        return PREFS.getBoolean(KEY_DARK, false);
    }

    public static void setDarkPreferred(boolean d) {
        PREFS.putBoolean(KEY_DARK, d);
    }

    /** Recalcula la paleta segun el modo (claro/oscuro). */
    public static void applyMode(boolean d) {
        dark = d;
        // Acento ambar comun a ambos modos.
        PRIMARY = new Color(0xF5, 0x9E, 0x0B);
        PRIMARY_DARK = new Color(0xD9, 0x77, 0x06);
        DANGER = new Color(0xEF, 0x44, 0x44);
        DANGER_DARK = new Color(0xDC, 0x26, 0x26);
        TEXT_ON_PRIMARY = Color.WHITE;

        if (d) {
            BACKGROUND = new Color(0x1E, 0x1E, 0x20);
            SURFACE = new Color(0x2A, 0x2B, 0x2E);
            LINE = new Color(0x3A, 0x3B, 0x40);
            TEXT = new Color(0xE5, 0xE7, 0xEB);
            TEXT_MUTED = new Color(0x9C, 0xA3, 0xAF);
            SELECTION = new Color(0x4A, 0x3A, 0x1A);
            ROW_EVEN = new Color(0x24, 0x25, 0x28);
            ROW_ODD = new Color(0x1E, 0x1E, 0x20);
        } else {
            BACKGROUND = new Color(0xFF, 0xFF, 0xFF);
            SURFACE = new Color(0xF1, 0xF5, 0xF9);
            LINE = new Color(0xE2, 0xE8, 0xF0);
            TEXT = new Color(0x1E, 0x29, 0x3B);
            TEXT_MUTED = new Color(0x64, 0x74, 0x8B);
            SELECTION = new Color(0xFE, 0xF3, 0xC7);
            ROW_EVEN = Color.WHITE;
            ROW_ODD = new Color(0xF8, 0xFA, 0xFC);
        }
    }

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
     * Debe invocarse despues de instalar el LaF (claro u oscuro) y Roboto.
     */
    public static void applyGlobalDefaults() {
        UIManager.put("defaultFont", fontRegular(13));

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
