/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.reminder.app.view;

import com.reminder.app.view.components.Action_button;
import com.reminder.app.view.components.CellJPanel;
import java.awt.Color;
import java.awt.Component;
import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

/**
 *
 * @author Jesus Gutierrez
 */
public class CellEditorTable extends DefaultCellEditor implements TableCellRenderer {

    private Action_button event_button;

    public CellEditorTable(JComboBox checkBox, Action_button event_button) {
        super(checkBox);
        this.event_button = event_button;
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        System.out.println("Se presiona");
        CellJPanel cellJPanel = new CellJPanel();
        cellJPanel.action_button(event_button, row);
        return cellJPanel;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        CellJPanel cellJPanel = new CellJPanel();
        if (isSelected) {
            cellJPanel.setBackground(Color.ORANGE);
        } else {
            cellJPanel.setBackground(Color.WHITE);
        }
        return cellJPanel;
    }
}
