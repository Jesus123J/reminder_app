/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.reminder.app.controller;

import com.reminder.app.model.ModelReminderData;
import com.reminder.app.view.ViewReminder;
import com.reminder.app.view.components.Action_button;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import raven.toast.Notifications;

/**
 *
 * @author Jesus Gutierrez
 */
public class ControllerReminder extends ModelReminderData implements ActionListener , Action_button {
    
    private ViewReminder viewReminder;

    public ControllerReminder() {
        
        this.viewReminder = new ViewReminder(this);
        super.init(viewReminder);
        viewReminder.buttonSaveData.addActionListener(this);
    }
   
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(viewReminder.buttonSaveData)) {
          Notifications.getInstance().show(Notifications.Type.SUCCESS, Notifications.Location.BOTTOM_RIGHT, "Se registro correctamente");
        }
    }

    @Override
    public void delete_index(int index) {
        System.out.println("Index -> "  + index);
    }  
}
