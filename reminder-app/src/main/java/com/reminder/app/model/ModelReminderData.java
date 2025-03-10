/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.reminder.app.model;

import com.reminder.app.view.ViewReminder;
import com.toedter.calendar.JDateChooser;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JPopupMenu;
import lombok.Data;

/**
 *
 * @author Jesus Gutierrez
 */
@Data
public class ModelReminderData {

    private String title;
    private String description;
    private LocalDate dateEnd;
    private LocalDate dateStart;

    //
    private JDateChooser dateChooser;

    public void init(ViewReminder viewReminder) {
        viewReminder.setLocationRelativeTo(null);
        viewReminder.setVisible(true);
    }

}
