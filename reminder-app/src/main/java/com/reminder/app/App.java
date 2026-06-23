package com.reminder.app;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.reminder.app.config.AppConfig;
import com.reminder.app.controller.ControllerReminder;
import com.reminder.app.view.ViewReminder;
import javax.security.auth.login.AppConfigurationEntry;

/**
 * Hello world!
 */
public class App {

    public static void main(String[] args) {
        FlatLightLaf.setup();
        AppConfig.createDocumentData();
        AppConfig config = new AppConfig();
        //Controller main
        ControllerReminder controllerReminder = new ControllerReminder();
    }
}
