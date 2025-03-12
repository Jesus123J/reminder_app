/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.reminder.app.config;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Jesus Gutierrez
 */
public final class AppConfig {

    public static final String PATH_DOCUMENT = "data.txr";

    public AppConfig() {
        try {
            throw new UnsupportedOperationException("you should not initialize the class");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

    }

    public static void createDocumentData() {

        Path path = Paths.get(PATH_DOCUMENT);

        //verification file
        if (Files.exists(path)) {
            System.out.println("Existe document");
            return;

        } else {
            try {
                Files.createFile(path);
            } catch (IOException ex) {
                System.out.println("A problem ocurred while creating the document");
            }
        }

    }

}
