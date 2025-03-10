/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.reminder.app.controller;

import com.reminder.app.model.ModelReminderData;
import com.reminder.app.view.ViewReminder;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 *
 * @author Jesus Gutierrez
 */
public class ControllerReminder extends ModelReminderData implements ActionListener {
    
    private ViewReminder viewReminder;

    public ControllerReminder(ViewReminder viewReminder) {
        this.viewReminder = viewReminder;
        super.init(viewReminder);
    }
   
    
    @Override
    public void actionPerformed(ActionEvent e) {
        
    }
    
}
