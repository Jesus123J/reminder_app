package com.reminder.app.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Configuracion global de la aplicacion.
 *
 * Clase utilitaria (no instanciable) que centraliza la ruta del almacenamiento
 * local y se asegura de que el archivo de datos exista al iniciar.
 *
 * @author Jesus Gutierrez
 */
public final class AppConfig {

    private static final Logger LOGGER = Logger.getLogger(AppConfig.class.getName());

    /** Archivo local donde se persisten los recordatorios. */
    public static final String PATH_DOCUMENT = "data.txr";

    private AppConfig() {
        // Clase utilitaria: no debe instanciarse.
    }

    /**
     * Crea el archivo de datos si todavia no existe. Es idempotente:
     * si ya existe, no hace nada.
     */
    public static void createDocumentData() {
        Path path = Paths.get(PATH_DOCUMENT);
        if (Files.exists(path)) {
            LOGGER.log(Level.INFO, "Archivo de datos encontrado: {0}", path.toAbsolutePath());
            return;
        }
        try {
            Files.createFile(path);
            LOGGER.log(Level.INFO, "Archivo de datos creado: {0}", path.toAbsolutePath());
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "No se pudo crear el archivo de datos", ex);
        }
    }
}
