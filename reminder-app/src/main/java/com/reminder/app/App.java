package com.reminder.app;

import com.formdev.flatlaf.FlatDarkLaf;
import com.reminder.app.controller.ControllerReminder;
import com.reminder.app.view.ViewReminder;

/**
 * Hello world!
 */
public class App {
    public static void main(String[] args) {
        FlatDarkLaf.setup();
        ControllerReminder controllerReminder = new ControllerReminder(new ViewReminder());
    }
}
