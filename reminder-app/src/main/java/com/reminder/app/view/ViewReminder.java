/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package com.reminder.app.view;

import com.reminder.app.config.Theme;
import com.reminder.app.service.SoundPlayer;
import com.reminder.app.service.integration.IntegrationManager;
import com.reminder.app.service.integration.ReminderIntegration;
import com.reminder.app.util.PanelRound;
import com.reminder.app.view.components.Action_button;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.time.LocalDate;
import java.time.LocalTime;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import raven.datetime.component.date.DatePicker;
import raven.datetime.component.time.TimePicker;

/**
 *
 * @author Jesus Gutierrez
 */
public final class ViewReminder extends javax.swing.JFrame {

    /**
     * Creates new form ViewReminder
     */
    private Action_button event_button;

    /** Selectores de fecha/hora; se conservan para leer el valor elegido. */
    private DatePicker datePicker;
    private TimePicker timePicker;

    /** Controles extra: prioridad, categoria y recurrencia. */
    private JComboBox<String> priorityCombo;
    private javax.swing.JTextField categoryField;
    private JComboBox<String> recurrenceCombo;
    /** Recordatorios actualmente pintados (para colorear por prioridad). */
    private java.util.List<com.reminder.app.model.Reminder> rowData = java.util.Collections.emptyList();

    /** Navegacion de una sola ventana (recordatorios / notas). */
    private final java.awt.CardLayout cards = new java.awt.CardLayout();
    private JPanel cardRoot;
    private NotesPanel notesPanel;

    public ViewReminder(Action_button event_button) {
        this.event_button = event_button;
        initComponents();
        applyTheme();
        setupPriorityCategory();
        setupTableModel();
        cellRenderTable();
        settingDate();
        wrapInCards();
        // Ensancha la ventana 10px sobre el tamano calculado por pack().
        setSize(getWidth() + 10, getHeight());
    }

    /** Envuelve el contenido en un CardLayout para alternar recordatorios/notas. */
    private void wrapInCards() {
        cardRoot = new JPanel(cards);
        getContentPane().remove(jPanel1);
        cardRoot.add(jPanel1, "reminders");
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(cardRoot, BorderLayout.CENTER);
        cards.show(cardRoot, "reminders");
    }

    /** Crea el panel de notas (una vez) y lo agrega como tarjeta. */
    public void attachNotes(com.reminder.app.repository.NoteRepository repo) {
        if (notesPanel == null) {
            notesPanel = new NotesPanel(repo, () -> cards.show(cardRoot, "reminders"));
            cardRoot.add(notesPanel, "notes");
        }
    }

    /** Muestra la tarjeta de notas dentro de la misma ventana. */
    public void showNotes() {
        if (notesPanel != null) {
            cards.show(cardRoot, "notes");
        }
    }

    /** Anade los controles de prioridad y categoria a la barra de fecha/hora. */
    private void setupPriorityCategory() {
        priorityCombo = new JComboBox<>(new String[]{"Alta", "Media", "Baja"});
        priorityCombo.setSelectedItem("Media");
        priorityCombo.setFont(Theme.fontRegular(13));

        categoryField = new javax.swing.JTextField(8);
        categoryField.putClientProperty("JTextField.placeholderText", "Categoría");
        categoryField.setFont(Theme.fontRegular(13));

        recurrenceCombo = new JComboBox<>(new String[]{"No repetir", "Diaria", "Semanal", "Mensual"});
        recurrenceCombo.setFont(Theme.fontRegular(13));

        // Reorganiza panelRound1 en una fila fluida conservando date/time/combo.
        panelRound1.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 8));
        panelRound1.add(formLabel("Prioridad:"));
        panelRound1.add(priorityCombo);
        panelRound1.add(categoryField);
        panelRound1.add(formLabel("Repetir:"));
        panelRound1.add(recurrenceCombo);
        panelRound1.revalidate();
    }

    private JLabel formLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.fontBold(12));
        l.setForeground(Theme.TEXT_MUTED);
        return l;
    }

    /**
     * Aplica la paleta y tipografias del {@link Theme} sobre los componentes,
     * corrigiendo el bajo contraste de las "letras negras" y unificando colores.
     */
    private void applyTheme() {
        getContentPane().setBackground(Theme.BACKGROUND);
        jPanel1.setBackground(Theme.BACKGROUND);
        // Respiracion alrededor del contenido (look moderno).
        jPanel1.setBorder(new EmptyBorder(18, 18, 18, 18));

        // Tarjetas con superficie clara y borde sutil ya redondeado.
        for (PanelRound panel : new PanelRound[]{panelRound1, panelRound2}) {
            panel.setBackground(Theme.SURFACE);
        }
        panelRound3.setBackground(Theme.BACKGROUND);

        // Tabla dentro de una "tarjeta" limpia: sin borde duro y con scroll suave.
        jScrollPane1.setBorder(new EmptyBorder(0, 0, 0, 0));
        jScrollPane1.getViewport().setBackground(Theme.BACKGROUND);
        jScrollPane1.setBackground(Theme.BACKGROUND);

        // Campos de fecha/hora/combo: texto oscuro legible sobre la tarjeta.
        for (Component c : new Component[]{jFormattedDateStart, jFormattedTextField1, jComboBox1}) {
            c.setBackground(Theme.SURFACE);
            c.setForeground(Theme.TEXT);
            c.setFont(Theme.fontRegular(14));
        }

        // Titulo y descripcion.
        textField1.setForeground(Theme.TEXT);
        textField1.setBackground(Theme.SURFACE);
        textField1.setFont(Theme.fontRegular(15));
        jTextPane1.setForeground(Theme.TEXT);
        jTextPane1.setFont(Theme.fontRegular(14));

        // Botones de accion.
        styleButton(buttonSaveData, Theme.PRIMARY, Theme.TEXT_ON_PRIMARY);
        styleButton(button1, Theme.DANGER, Theme.TEXT_ON_PRIMARY);
        styleButton(button2, Theme.PRIMARY, Theme.TEXT_ON_PRIMARY);
    }

    private void styleButton(com.reminder.app.util.Button button, Color bg, Color fg) {
        button.setBackground(bg);
        button.setForeground(fg);
        button.setFont(Theme.fontBold(13));
    }

    /** Crea un boton de la barra superior con estilo uniforme. */
    private JButton barButton(String text, java.awt.event.ActionListener action) {
        JButton b = new JButton(text);
        b.setFont(Theme.fontBold(13));
        b.setForeground(Theme.TEXT);
        b.setFocusPainted(false);
        b.setMargin(new java.awt.Insets(6, 14, 6, 14));
        if (action != null) {
            b.addActionListener(action);
        }
        return b;
    }

    /**
     * Crea la barra de menu con las opciones de integraciones (extensiones).
     * El usuario activa/desactiva las que puede; las gestionadas por
     * configuracion externa se muestran solo informativas.
     */
    public void installIntegrationsMenu(IntegrationManager manager, SoundPlayer sound,
                                        java.util.function.BiConsumer<String, String> filterHandler) {
        JMenuBar menuBar = new JMenuBar();
        menuBar.setBorder(new EmptyBorder(4, 8, 4, 8));

        // Botones de la barra con texto limpio (sin emoji que se ven como cuadros).
        menuBar.add(barButton("Notas", e -> showNotes()));
        menuBar.add(javax.swing.Box.createHorizontalStrut(6));
        menuBar.add(barButton("Integraciones", e -> openIntegrationsDialog(manager)));
        menuBar.add(javax.swing.Box.createHorizontalStrut(6));
        menuBar.add(barButton("Sonido", e -> openSoundDialog(sound)));
        menuBar.add(javax.swing.Box.createHorizontalStrut(6));

        final JButton themeBtn = barButton(Theme.dark ? "Tema: Claro" : "Tema: Oscuro", null);
        themeBtn.addActionListener(e -> {
            toggleTheme();
            themeBtn.setText(Theme.dark ? "Tema: Claro" : "Tema: Oscuro");
        });
        menuBar.add(themeBtn);

        menuBar.add(javax.swing.Box.createHorizontalGlue());

        // Busqueda + filtro (a la derecha).
        JComboBox<String> filterCombo = new JComboBox<>(
                new String[]{"Todos", "Hoy", "Próximos", "Vencidos"});
        filterCombo.setFont(Theme.fontRegular(13));
        javax.swing.JTextField search = new javax.swing.JTextField(12);
        search.putClientProperty("JTextField.placeholderText", "Buscar...");
        search.setFont(Theme.fontRegular(13));
        search.setMaximumSize(new Dimension(200, 30));

        Runnable fire = () -> filterHandler.accept(
                search.getText().trim(), String.valueOf(filterCombo.getSelectedItem()));
        search.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { fire.run(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { fire.run(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { fire.run(); }
        });
        filterCombo.addActionListener(e -> fire.run());

        JLabel searchLbl = new JLabel("Buscar: ");
        searchLbl.setFont(Theme.fontRegular(13));
        searchLbl.setForeground(Theme.TEXT_MUTED);
        menuBar.add(searchLbl);
        menuBar.add(search);
        menuBar.add(javax.swing.Box.createHorizontalStrut(8));
        menuBar.add(filterCombo);

        setJMenuBar(menuBar);
        // Recalcula el tamano incluyendo la barra (evita recortes).
        pack();
        // La ventana debe ser al menos tan ancha como la barra de botones, o
        // estos se ocultan. Ajustamos tamano y minimo a ese ancho.
        int barWidth = menuBar.getPreferredSize().width + 40;
        int width = Math.max(getWidth() + 10, barWidth);
        setSize(width, getHeight());
        setMinimumSize(new Dimension(barWidth, getHeight()));
        setLocationRelativeTo(null);
    }

    /** Alterna entre tema claro y oscuro en caliente y re-tematiza la UI. */
    private void toggleTheme() {
        boolean newDark = !Theme.dark;
        if (newDark) {
            com.formdev.flatlaf.FlatDarkLaf.setup();
        } else {
            com.formdev.flatlaf.FlatLightLaf.setup();
        }
        Theme.setDarkPreferred(newDark);
        Theme.applyMode(newDark);
        Theme.applyGlobalDefaults();
        javax.swing.SwingUtilities.updateComponentTreeUI(this);
        applyTheme();
        cellRenderTable();
        repaint();
    }

    /** Dialogo para activar/silenciar el sonido y elegir un .wav. */
    private void openSoundDialog(SoundPlayer sound) {
        JCheckBox enabled = new JCheckBox("Activar sonido de aviso", sound.isEnabled());
        JLabel current = new JLabel(soundFileLabel(sound));
        JButton choose = new JButton("Elegir .wav...");
        choose.addActionListener(e -> {
            javax.swing.JFileChooser fc = new javax.swing.JFileChooser();
            fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Audio WAV", "wav"));
            if (fc.showOpenDialog(this) == javax.swing.JFileChooser.APPROVE_OPTION) {
                sound.setSoundFile(fc.getSelectedFile().getAbsolutePath());
                current.setText(soundFileLabel(sound));
            }
        });
        JButton useBeep = new JButton("Usar beep");
        useBeep.addActionListener(e -> {
            sound.setSoundFile("");
            current.setText(soundFileLabel(sound));
        });
        JButton test = new JButton("Probar");
        test.addActionListener(e -> sound.play());

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(4, 4, 4, 4));
        enabled.addActionListener(e -> sound.setEnabled(enabled.isSelected()));
        for (java.awt.Component c : new java.awt.Component[]{enabled, current, choose, useBeep, test}) {
            ((javax.swing.JComponent) c).setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(c);
            panel.add(javax.swing.Box.createVerticalStrut(8));
        }
        JOptionPane.showMessageDialog(this, panel, "Sonido", JOptionPane.PLAIN_MESSAGE);
    }

    private String soundFileLabel(SoundPlayer sound) {
        String f = sound.getSoundFile();
        return (f == null || f.isEmpty())
                ? "Sonido actual: beep del sistema"
                : "Sonido actual: " + new java.io.File(f).getName();
    }

    /** Dialogo modal e integrado para activar/desactivar las extensiones. */
    private void openIntegrationsDialog(IntegrationManager manager) {
        final JDialog dialog = new JDialog(this, "Integraciones / Extensiones", true);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(Theme.BACKGROUND);
        content.setBorder(new EmptyBorder(18, 20, 18, 20));

        JLabel header = new JLabel("Activa las extensiones que quieras");
        header.setFont(Theme.fontBold(16));
        header.setForeground(Theme.TEXT);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(header);
        content.add(javax.swing.Box.createVerticalStrut(14));

        for (ReminderIntegration integration : manager.getIntegrations()) {
            content.add(buildIntegrationRow(integration));
            content.add(javax.swing.Box.createVerticalStrut(10));
        }

        JButton close = new JButton("Cerrar");
        close.setFont(Theme.fontBold(13));
        close.setBackground(Theme.PRIMARY);
        close.setForeground(Theme.TEXT_ON_PRIMARY);
        close.setFocusPainted(false);
        close.setAlignmentX(Component.LEFT_ALIGNMENT);
        close.addActionListener(e -> dialog.dispose());
        content.add(javax.swing.Box.createVerticalStrut(6));
        content.add(close);

        dialog.setContentPane(content);
        dialog.pack();
        dialog.setMinimumSize(new Dimension(420, dialog.getHeight()));
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private JPanel buildIntegrationRow(ReminderIntegration integration) {
        JPanel row = new JPanel(new java.awt.BorderLayout(10, 0));
        row.setBackground(Theme.SURFACE);
        row.setBorder(new EmptyBorder(10, 14, 10, 14));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));

        JPanel texts = new JPanel();
        texts.setLayout(new BoxLayout(texts, BoxLayout.Y_AXIS));
        texts.setOpaque(false);
        JLabel name = new JLabel(integration.name());
        name.setFont(Theme.fontBold(14));
        name.setForeground(Theme.TEXT);
        String desc = integration.description().isEmpty()
                ? "Integración avanzada (requiere configuración)."
                : integration.description();
        JLabel sub = new JLabel(desc);
        sub.setFont(Theme.fontRegular(12));
        sub.setForeground(Theme.TEXT_MUTED);
        texts.add(name);
        texts.add(sub);
        row.add(texts, java.awt.BorderLayout.CENTER);

        if (integration.canToggle()) {
            JCheckBox toggle = new JCheckBox();
            toggle.setSelected(integration.isEnabled());
            toggle.setOpaque(false);
            toggle.addActionListener(e -> integration.setEnabled(toggle.isSelected()));
            row.add(toggle, java.awt.BorderLayout.EAST);
        } else {
            JLabel state = new JLabel(integration.isEnabled() ? "activa" : "sin configurar");
            state.setFont(Theme.fontRegular(12));
            state.setForeground(integration.isEnabled() ? Theme.PRIMARY_DARK : Theme.TEXT_MUTED);
            row.add(state, java.awt.BorderLayout.EAST);
        }
        return row;
    }

    public void settingDate() {
        datePicker = new DatePicker();
        timePicker = new TimePicker();
        timePicker.set24HourView(true);
        timePicker.setEditor(jFormattedTextField1);
        datePicker.setEditor(jFormattedDateStart);
        // Valores por defecto utiles: hoy y la hora actual.
        datePicker.now();
        timePicker.setSelectedTime(LocalTime.now().withSecond(0).withNano(0));
    }

    // ----- API para el controlador: lectura del formulario -----

    public String getTitleInput() {
        return textField1.getText().trim();
    }

    public String getDescriptionInput() {
        return jTextPane1.getText().trim();
    }

    public LocalDate getSelectedDate() {
        return datePicker.isDateSelected() ? datePicker.getSelectedDate() : null;
    }

    public LocalTime getSelectedTime() {
        return timePicker.isTimeSelected() ? timePicker.getSelectedTime() : null;
    }

    /**
     * Traduce la etiqueta del combo a minutos de antelacion (cuanto ANTES de la
     * fecha/hora se avisa). "A la hora exacta" = 0.
     */
    public int getAdvanceMinutes() {
        String label = String.valueOf(jComboBox1.getSelectedItem());
        switch (label) {
            case "A la hora exacta": return 0;
            case "5 minutos antes":  return 5;
            case "10 minutos antes": return 10;
            case "30 minutos antes": return 30;
            case "1 hora antes":     return 60;
            case "1 día antes":      return 1440;
            default:                 return 0;
        }
    }

    /** Selecciona en el combo la etiqueta correspondiente a los minutos dados. */
    public void setAdvanceMinutes(int minutes) {
        String label;
        switch (minutes) {
            case 5:    label = "5 minutos antes";  break;
            case 10:   label = "10 minutos antes"; break;
            case 30:   label = "30 minutos antes"; break;
            case 60:   label = "1 hora antes";     break;
            case 1440: label = "1 día antes";      break;
            default:   label = "A la hora exacta"; break;
        }
        jComboBox1.setSelectedItem(label);
    }

    /** Prioridad elegida en el formulario. */
    public com.reminder.app.model.Reminder.Priority getPriority() {
        String s = String.valueOf(priorityCombo.getSelectedItem());
        switch (s) {
            case "Alta": return com.reminder.app.model.Reminder.Priority.ALTA;
            case "Baja": return com.reminder.app.model.Reminder.Priority.BAJA;
            default:     return com.reminder.app.model.Reminder.Priority.MEDIA;
        }
    }

    public String getCategory() {
        return categoryField.getText().trim();
    }

    public com.reminder.app.model.Reminder.Recurrence getRecurrence() {
        String s = String.valueOf(recurrenceCombo.getSelectedItem());
        switch (s) {
            case "Diaria":  return com.reminder.app.model.Reminder.Recurrence.DIARIA;
            case "Semanal": return com.reminder.app.model.Reminder.Recurrence.SEMANAL;
            case "Mensual": return com.reminder.app.model.Reminder.Recurrence.MENSUAL;
            default:        return com.reminder.app.model.Reminder.Recurrence.NINGUNA;
        }
    }

    /** Carga un recordatorio en el formulario para editarlo. */
    public void loadIntoForm(String title, String description, LocalDate date,
                             LocalTime time, int advanceMinutes,
                             com.reminder.app.model.Reminder.Priority priority, String category,
                             com.reminder.app.model.Reminder.Recurrence recurrence) {
        textField1.setText(title == null ? "" : title);
        jTextPane1.setText(description == null ? "" : description);
        if (date != null) {
            datePicker.setSelectedDate(date);
        }
        if (time != null) {
            timePicker.setSelectedTime(time);
        }
        setAdvanceMinutes(advanceMinutes);
        priorityCombo.setSelectedItem(priority == com.reminder.app.model.Reminder.Priority.ALTA ? "Alta"
                : priority == com.reminder.app.model.Reminder.Priority.BAJA ? "Baja" : "Media");
        categoryField.setText(category == null ? "" : category);
        recurrenceCombo.setSelectedItem(
                recurrence == com.reminder.app.model.Reminder.Recurrence.DIARIA ? "Diaria"
                : recurrence == com.reminder.app.model.Reminder.Recurrence.SEMANAL ? "Semanal"
                : recurrence == com.reminder.app.model.Reminder.Recurrence.MENSUAL ? "Mensual" : "No repetir");
    }

    /** Registra un manejador que recibe el indice de la fila seleccionada. */
    public void setRowSelectionHandler(java.util.function.IntConsumer handler) {
        jTable1.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = jTable1.getSelectedRow();
                if (row >= 0) {
                    handler.accept(row);
                }
            }
        });
    }

    /** Boton EDITAR para que el controlador registre su accion. */
    public com.reminder.app.util.Button getEditButton() {
        return button2;
    }

    /** Fila seleccionada actualmente (o -1 si ninguna). */
    public int getSelectedRow() {
        return jTable1.getSelectedRow();
    }

    /**
     * Instala el menu contextual (clic derecho) de cada recordatorio con las
     * acciones Editar, Posponer y Eliminar. Sustituye al boton de eliminar en la
     * celda (que no encaja con el diseño de tarjetas).
     */
    public void installRowMenu(java.util.function.IntConsumer onEdit,
                               java.util.function.BiConsumer<Integer, Integer> onSnooze,
                               java.util.function.IntConsumer onDelete) {
        javax.swing.JPopupMenu popup = new javax.swing.JPopupMenu();

        javax.swing.JMenuItem edit = new javax.swing.JMenuItem("Editar");
        edit.addActionListener(e -> withRow(onEdit));
        popup.add(edit);
        popup.addSeparator();

        for (int m : new int[]{10, 30, 60}) {
            javax.swing.JMenuItem item = new javax.swing.JMenuItem("Posponer " + m + " min");
            final int mins = m;
            item.addActionListener(e -> {
                int row = jTable1.getSelectedRow();
                if (row >= 0) {
                    onSnooze.accept(row, mins);
                }
            });
            popup.add(item);
        }
        popup.addSeparator();

        javax.swing.JMenuItem delete = new javax.swing.JMenuItem("Eliminar");
        delete.setForeground(Theme.DANGER);
        delete.addActionListener(e -> withRow(onDelete));
        popup.add(delete);

        // Seleccionar la fila bajo el cursor al hacer clic derecho.
        jTable1.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                maybeSelect(e);
            }

            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                maybeSelect(e);
            }

            private void maybeSelect(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int row = jTable1.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        jTable1.setRowSelectionInterval(row, row);
                    }
                }
            }
        });
        jTable1.setComponentPopupMenu(popup);
    }

    private void withRow(java.util.function.IntConsumer action) {
        int row = jTable1.getSelectedRow();
        if (row >= 0) {
            action.accept(row);
        }
    }

    /** Limpia el formulario tras guardar. */
    public void clearForm() {
        textField1.setText("");
        jTextPane1.setText("");
        datePicker.now();
        timePicker.setSelectedTime(LocalTime.now().withSecond(0).withNano(0));
        jComboBox1.setSelectedIndex(0);
        priorityCombo.setSelectedItem("Media");
        categoryField.setText("");
        recurrenceCombo.setSelectedItem("No repetir");
        jTable1.clearSelection();
    }

    /** Boton "ELIMINAR TODO" para que el controlador registre su accion. */
    public com.reminder.app.util.Button getDeleteAllButton() {
        return button1;
    }

    // ----- API para el controlador: pintado de la tabla -----

    /** Instala un modelo de tabla tipo "tarjetas": 1 columna rica + accion. */
    private void setupTableModel() {
        DefaultTableModel model = new DefaultTableModel(
                new Object[]{"Recordatorios"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // las acciones van en el menu contextual
            }
        };
        jTable1.setModel(model);
    }

    /** Refresca la tabla con la lista de recordatorios indicada (tarjetas HTML). */
    public void loadReminders(java.util.List<com.reminder.app.model.Reminder> reminders) {
        this.rowData = reminders;
        DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
        model.setRowCount(0);
        for (com.reminder.app.model.Reminder r : reminders) {
            model.addRow(new Object[]{buildCardHtml(r)});
        }
        // Reaplicar render/editor de columnas tras cambiar el modelo.
        cellRenderTable();
    }

    /** Construye la "tarjeta" HTML de un recordatorio (titulo + meta en 2 lineas). */
    private String buildCardHtml(com.reminder.app.model.Reminder r) {
        String titleColor = toHex(priorityColorOf(r));
        String muted = toHex(Theme.TEXT_MUTED);
        String accent = toHex(Theme.PRIMARY_DARK);

        String prioLabel;
        switch (r.getPriority()) {
            case ALTA: prioLabel = "Alta"; break;
            case BAJA: prioLabel = "Baja"; break;
            default:   prioLabel = "Media"; break;
        }

        StringBuilder meta = new StringBuilder();
        meta.append(dayBucket(r.getDate()));
        if (r.getTime() != null) {
            meta.append(" · ").append(r.getTime());
        }
        if (r.getCategory() != null && !r.getCategory().isBlank()) {
            meta.append(" · ").append(esc(r.getCategory()));
        }
        if (r.isRecurring()) {
            meta.append(" · repite");
        }
        String desc = r.getDescription() == null ? "" : r.getDescription().replace("\n", " ").trim();
        if (!desc.isBlank()) {
            meta.append(" · ").append(esc(desc));
        }

        return "<html><div style='padding:3px 2px'>"
                + "<font color='" + titleColor + "' size='4'><b>" + esc(r.getTitle()) + "</b></font>"
                + "&nbsp;&nbsp;<font color='" + accent + "' size='2'>● " + prioLabel + "</font>"
                + "<br><font color='" + muted + "' size='2'>" + meta + "</font>"
                + "</div></html>";
    }

    private Color priorityColorOf(com.reminder.app.model.Reminder r) {
        switch (r.getPriority()) {
            case ALTA: return Theme.DANGER;
            case BAJA: return Theme.TEXT_MUTED;
            default:   return Theme.TEXT;
        }
    }

    private String toHex(Color c) {
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }

    private String esc(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /** Clasifica una fecha en un grupo legible relativo a hoy. */
    private String dayBucket(LocalDate date) {
        if (date == null) {
            return "Sin fecha";
        }
        LocalDate today = LocalDate.now();
        if (date.isBefore(today)) {
            return "Vencido";
        }
        if (date.equals(today)) {
            return "Hoy";
        }
        if (date.equals(today.plusDays(1))) {
            return "Mañana";
        }
        if (!date.isAfter(today.plusDays(7))) {
            return "Esta semana";
        }
        return "Después";
    }

    public void cellRenderTable() {
        // Sin cabecera: aspecto de lista/tarjetas, no de tabla clasica.
        jTable1.setTableHeader(null);

        // Tarjeta: contenido HTML (titulo + meta) con zebra y borde redondeado.
        DefaultTableCellRenderer cardRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel c = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setVerticalAlignment(JLabel.CENTER);
                c.setBorder(new EmptyBorder(6, 14, 6, 10));
                c.setBackground(isSelected ? Theme.SELECTION : (row % 2 == 0 ? Theme.ROW_EVEN : Theme.ROW_ODD));
                return c;
            }
        };
        jTable1.getColumnModel().getColumn(0).setCellRenderer(cardRenderer);

        jTable1.setSelectionBackground(Theme.SELECTION);
        jTable1.setSelectionForeground(Theme.TEXT);
        jTable1.setRowHeight(64);
        jTable1.setShowGrid(false);
        jTable1.setIntercellSpacing(new Dimension(0, 6));
        jTable1.setBackground(Theme.BACKGROUND);
        jTable1.setForeground(Theme.TEXT);
        jTable1.setFillsViewportHeight(true);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        buttonSaveData = new com.reminder.app.util.Button();
        panelRound1 = new com.reminder.app.util.PanelRound();
        jFormattedTextField1 = new javax.swing.JFormattedTextField();
        jFormattedDateStart = new javax.swing.JFormattedTextField();
        jComboBox1 = new javax.swing.JComboBox<>();
        panelRound2 = new com.reminder.app.util.PanelRound();
        textField1 = new com.reminder.app.util.TextField();
        panelRound3 = new com.reminder.app.util.PanelRound();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTextPane1 = new javax.swing.JTextPane();
        button2 = new com.reminder.app.util.Button();
        button1 = new com.reminder.app.util.Button();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Recordatorios");

        jPanel1.setBackground(new java.awt.Color(255, 255, 255));

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null},
                {null, null, null},
                {null, null, null},
                {null, null, null}
            },
            new String [] {
                "TITULO", "DESCRIPCIÓN", " "
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, true
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane1.setViewportView(jTable1);

        buttonSaveData.setBackground(new java.awt.Color(255, 204, 153));
        buttonSaveData.setForeground(new java.awt.Color(51, 51, 51));
        buttonSaveData.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icon-button/save_button.png"))); // NOI18N
        buttonSaveData.setText("GUARDAR");
        buttonSaveData.setFont(new java.awt.Font("Sitka Text", 0, 12)); // NOI18N
        buttonSaveData.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        buttonSaveData.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonSaveDataActionPerformed(evt);
            }
        });

        panelRound1.setBackground(new java.awt.Color(204, 204, 204));
        panelRound1.setRoundBottomLeft(20);
        panelRound1.setRoundBottomRight(20);
        panelRound1.setRoundTopLeft(20);
        panelRound1.setRoundTopRight(20);

        jFormattedTextField1.setBackground(new java.awt.Color(204, 204, 204));
        jFormattedTextField1.setBorder(null);

        jFormattedDateStart.setBackground(new java.awt.Color(204, 204, 204));
        jFormattedDateStart.setBorder(null);

        jComboBox1.setBackground(new java.awt.Color(204, 204, 204));
        jComboBox1.setForeground(new java.awt.Color(0, 0, 0));
        jComboBox1.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "A la hora exacta", "5 minutos antes", "10 minutos antes", "30 minutos antes", "1 hora antes", "1 día antes" }));
        jComboBox1.setBorder(null);

        javax.swing.GroupLayout panelRound1Layout = new javax.swing.GroupLayout(panelRound1);
        panelRound1.setLayout(panelRound1Layout);
        panelRound1Layout.setHorizontalGroup(
            panelRound1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelRound1Layout.createSequentialGroup()
                .addGap(29, 29, 29)
                .addComponent(jFormattedDateStart, javax.swing.GroupLayout.PREFERRED_SIZE, 123, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jFormattedTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, 155, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        panelRound1Layout.setVerticalGroup(
            panelRound1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelRound1Layout.createSequentialGroup()
                .addGap(5, 5, 5)
                .addGroup(panelRound1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jFormattedTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jFormattedDateStart, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        panelRound2.setBackground(new java.awt.Color(204, 204, 204));
        panelRound2.setRoundBottomLeft(20);
        panelRound2.setRoundBottomRight(20);
        panelRound2.setRoundTopLeft(20);
        panelRound2.setRoundTopRight(20);

        textField1.setBackground(new java.awt.Color(204, 204, 204));
        textField1.setForeground(new java.awt.Color(0, 0, 0));
        textField1.setCaretColor(new java.awt.Color(0, 0, 0));
        textField1.setLabelText("TITULO");
        textField1.setLineColor(new java.awt.Color(102, 102, 102));
        textField1.setSelectedTextColor(new java.awt.Color(255, 255, 255));
        textField1.setSelectionColor(new java.awt.Color(153, 255, 255));

        panelRound3.setBackground(new java.awt.Color(255, 255, 255));
        panelRound3.setRoundBottomLeft(5);
        panelRound3.setRoundBottomRight(5);
        panelRound3.setRoundTopLeft(5);
        panelRound3.setRoundTopRight(5);

        jScrollPane2.setBorder(null);

        jTextPane1.setBackground(new java.awt.Color(255, 255, 255));
        jTextPane1.setBorder(null);
        jTextPane1.setForeground(new java.awt.Color(0, 0, 0));
        jTextPane1.setToolTipText("");
        jTextPane1.setCaretColor(new java.awt.Color(0, 0, 0));
        jScrollPane2.setViewportView(jTextPane1);

        javax.swing.GroupLayout panelRound3Layout = new javax.swing.GroupLayout(panelRound3);
        panelRound3.setLayout(panelRound3Layout);
        panelRound3Layout.setHorizontalGroup(
            panelRound3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelRound3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2)
                .addContainerGap())
        );
        panelRound3Layout.setVerticalGroup(
            panelRound3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelRound3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 69, Short.MAX_VALUE)
                .addContainerGap())
        );

        button2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icon-button/button_edit.png"))); // NOI18N
        button2.setText("EDITAR");

        javax.swing.GroupLayout panelRound2Layout = new javax.swing.GroupLayout(panelRound2);
        panelRound2.setLayout(panelRound2Layout);
        panelRound2Layout.setHorizontalGroup(
            panelRound2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelRound2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelRound2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(panelRound3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(panelRound2Layout.createSequentialGroup()
                        .addComponent(textField1, javax.swing.GroupLayout.PREFERRED_SIZE, 204, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(button2, javax.swing.GroupLayout.PREFERRED_SIZE, 196, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        panelRound2Layout.setVerticalGroup(
            panelRound2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelRound2Layout.createSequentialGroup()
                .addContainerGap(8, Short.MAX_VALUE)
                .addGroup(panelRound2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(textField1, javax.swing.GroupLayout.PREFERRED_SIZE, 59, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(button2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(panelRound3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        button1.setBackground(new java.awt.Color(255, 204, 51));
        button1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icon-button/delete_all_button.png"))); // NOI18N
        button1.setText("ELIMINAR TODO");
        button1.setFont(new java.awt.Font("Sitka Text", 0, 12)); // NOI18N
        button1.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 455, Short.MAX_VALUE)
                    .addComponent(panelRound2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(buttonSaveData, javax.swing.GroupLayout.PREFERRED_SIZE, 174, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(button1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(panelRound1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(buttonSaveData, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(button1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(3, 3, 3)))
                .addGap(5, 5, 5)
                .addComponent(panelRound1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(panelRound2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 133, Short.MAX_VALUE)
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void buttonSaveDataActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonSaveDataActionPerformed

    }//GEN-LAST:event_buttonSaveDataActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private com.reminder.app.util.Button button1;
    private com.reminder.app.util.Button button2;
    public com.reminder.app.util.Button buttonSaveData;
    private javax.swing.JComboBox<String> jComboBox1;
    public javax.swing.JFormattedTextField jFormattedDateStart;
    private javax.swing.JFormattedTextField jFormattedTextField1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTable jTable1;
    private javax.swing.JTextPane jTextPane1;
    private com.reminder.app.util.PanelRound panelRound1;
    private com.reminder.app.util.PanelRound panelRound2;
    private com.reminder.app.util.PanelRound panelRound3;
    private com.reminder.app.util.TextField textField1;
    // End of variables declaration//GEN-END:variables
}
