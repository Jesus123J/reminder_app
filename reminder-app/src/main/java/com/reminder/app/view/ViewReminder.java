/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package com.reminder.app.view;

import com.reminder.app.config.Theme;
import com.reminder.app.service.integration.IntegrationManager;
import com.reminder.app.service.integration.ReminderIntegration;
import com.reminder.app.util.PanelRound;
import com.reminder.app.view.components.Action_button;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.time.LocalDate;
import java.time.LocalTime;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
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

    public ViewReminder(Action_button event_button) {
        this.event_button = event_button;
        initComponents();
        applyTheme();
        setupTableModel();
        cellRenderTable();
        settingDate();
        // Ensancha la ventana 10px sobre el tamano calculado por pack().
        setSize(getWidth() + 10, getHeight());
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

    /**
     * Crea la barra de menu con las opciones de integraciones (extensiones).
     * El usuario activa/desactiva las que puede; las gestionadas por
     * configuracion externa se muestran solo informativas.
     */
    public void installIntegrationsMenu(IntegrationManager manager) {
        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("⚙  Integraciones");
        menu.setFont(Theme.fontBold(13));

        for (ReminderIntegration integration : manager.getIntegrations()) {
            if (integration.canToggle()) {
                JCheckBoxMenuItem item = new JCheckBoxMenuItem(integration.name(), integration.isEnabled());
                item.setToolTipText(integration.description());
                item.addActionListener(e -> integration.setEnabled(item.isSelected()));
                menu.add(item);
            }
        }

        menu.addSeparator();
        for (ReminderIntegration integration : manager.getIntegrations()) {
            if (!integration.canToggle()) {
                String estado = integration.isEnabled() ? "activa" : "sin configurar";
                JMenuItem info = new JMenuItem(integration.name() + "  —  " + estado);
                info.setEnabled(false);
                menu.add(info);
            }
        }

        menu.addSeparator();
        JMenuItem about = new JMenuItem("¿Cómo funcionan?");
        about.addActionListener(e -> showIntegrationsHelp(manager));
        menu.add(about);

        menuBar.add(menu);
        setJMenuBar(menuBar);
        // Recalcula el tamano incluyendo la barra de menu (evita recortes) y
        // conserva el extra de ancho; recentra la ventana.
        pack();
        setSize(getWidth() + 10, getHeight());
        setLocationRelativeTo(null);
    }

    private void showIntegrationsHelp(IntegrationManager manager) {
        StringBuilder sb = new StringBuilder("<html><body style='width:320px'>");
        sb.append("<b>Extensiones disponibles</b><br><br>");
        for (ReminderIntegration integration : manager.getIntegrations()) {
            sb.append("• <b>").append(integration.name()).append("</b><br>")
              .append(integration.description().isEmpty()
                      ? "Integración avanzada (requiere configuración)."
                      : integration.description())
              .append("<br><br>");
        }
        sb.append("Actívalas desde el menú <i>Integraciones</i>. Las de tipo "
                + "<i>link</i> y <i>.ics</i> usan tu propia cuenta y no requieren tokens.");
        sb.append("</body></html>");
        JOptionPane.showMessageDialog(this, new JLabel(sb.toString()),
                "Integraciones", JOptionPane.INFORMATION_MESSAGE);
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

    /** Limpia el formulario tras guardar. */
    public void clearForm() {
        textField1.setText("");
        jTextPane1.setText("");
        datePicker.now();
        timePicker.setSelectedTime(LocalTime.now().withSecond(0).withNano(0));
        jComboBox1.setSelectedIndex(0);
    }

    /** Boton "ELIMINAR TODO" para que el controlador registre su accion. */
    public com.reminder.app.util.Button getDeleteAllButton() {
        return button1;
    }

    // ----- API para el controlador: pintado de la tabla -----

    /** Instala un modelo de tabla dinamico (sin filas hardcodeadas). */
    private void setupTableModel() {
        DefaultTableModel model = new DefaultTableModel(
                new Object[]{"TÍTULO", "DESCRIPCIÓN", " "}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 2; // solo la columna de accion (boton eliminar)
            }
        };
        jTable1.setModel(model);
    }

    /** Refresca la tabla con la lista de recordatorios indicada. */
    public void loadReminders(java.util.List<com.reminder.app.model.Reminder> reminders) {
        DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
        model.setRowCount(0);
        for (com.reminder.app.model.Reminder r : reminders) {
            String when = (r.getDate() == null ? "" : r.getDate())
                    + (r.getTime() == null ? "" : " " + r.getTime());
            String desc = r.getDescription() == null ? "" : r.getDescription().replace("\n", " ");
            if (!when.isBlank()) {
                desc = desc.isBlank() ? when : desc + "  ·  " + when;
            }
            model.addRow(new Object[]{r.getTitle(), desc, ""});
        }
        // Reaplicar render/editor de columnas tras cambiar el modelo.
        cellRenderTable();
    }

    public void cellRenderTable() {
        // Encabezado: fondo ambar con texto BLANCO en negrita (alto contraste).
        jTable1.getTableHeader().setDefaultRenderer(new TableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = new JLabel(value == null ? "" : value.toString());
                label.setOpaque(true);
                label.setHorizontalAlignment(JLabel.CENTER);
                label.setFont(Theme.fontBold(14));
                label.setBackground(Theme.PRIMARY);
                label.setForeground(Theme.TEXT_ON_PRIMARY);
                label.setBorder(new EmptyBorder(8, 8, 8, 8));
                return label;
            }
        });

        // Celdas de texto: color oscuro legible + filas alternadas (zebra).
        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel c = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setBorder(new EmptyBorder(0, 12, 0, 12));
                c.setFont(Theme.fontRegular(14));
                if (isSelected) {
                    c.setBackground(Theme.SELECTION);
                } else {
                    c.setBackground(row % 2 == 0 ? Theme.ROW_EVEN : Theme.ROW_ODD);
                }
                c.setForeground(Theme.TEXT);
                return c;
            }
        };
        jTable1.getColumnModel().getColumn(0).setCellRenderer(cellRenderer);
        jTable1.getColumnModel().getColumn(1).setCellRenderer(cellRenderer);

        jTable1.getColumnModel().getColumn(2).setCellEditor(new CellEditorTable(new JComboBox<>(), event_button));
        jTable1.getColumnModel().getColumn(2).setCellRenderer(new CellEditorTable(new JComboBox<>(), event_button));

        jTable1.setSelectionBackground(Theme.SELECTION);
        jTable1.setSelectionForeground(Theme.TEXT);
        jTable1.setRowHeight(42);
        jTable1.setShowVerticalLines(false);
        jTable1.setGridColor(Theme.LINE);
        jTable1.setBackground(Theme.BACKGROUND);
        jTable1.setForeground(Theme.TEXT);
        jTable1.getTableHeader().setPreferredSize(new Dimension(0, 38));
        jTable1.getTableHeader().setReorderingAllowed(false);
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
