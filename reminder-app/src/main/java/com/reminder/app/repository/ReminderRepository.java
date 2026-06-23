package com.reminder.app.repository;

import com.reminder.app.config.AppConfig;
import com.reminder.app.model.Reminder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Capa de persistencia de los recordatorios (DAO).
 *
 * Almacena los recordatorios en el archivo local definido por {@link AppConfig}
 * en texto plano, una linea por recordatorio, con campos separados por '|'. Los
 * campos de texto libre (titulo y descripcion) se guardan en Base64 para que
 * cualquier caracter del usuario -incluidos '|' o saltos de linea- no rompa el
 * formato. Mantiene una copia en memoria que se reescribe completa en cada
 * mutacion (suficiente y robusto para un volumen local pequeno).
 *
 * Formato por linea:
 *   id | base64(titulo) | base64(descripcion) | fecha | hora | antelacionMin | notificado
 *
 * @author Jesus Gutierrez
 */
public class ReminderRepository {

    private static final Logger LOGGER = Logger.getLogger(ReminderRepository.class.getName());

    private static final String FIELD_SEP = "|";
    private static final int FIELD_COUNT = 7;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE; // yyyy-MM-dd
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final Path path;
    private final List<Reminder> cache = new ArrayList<>();
    private long nextId = 1;

    public ReminderRepository() {
        this(Paths.get(AppConfig.PATH_DOCUMENT));
    }

    public ReminderRepository(Path path) {
        this.path = path;
        load();
    }

    /** Devuelve una copia ordenada por fecha/hora de vencimiento. */
    public synchronized List<Reminder> findAll() {
        List<Reminder> copy = new ArrayList<>(cache);
        copy.sort(Comparator.comparing(Reminder::dueAt));
        return copy;
    }

    /** Crea un nuevo recordatorio, lo persiste y lo devuelve con su id asignado. */
    public synchronized Reminder add(String title, String description,
                                     LocalDate date, LocalTime time, int advanceMinutes) {
        Reminder reminder = new Reminder(nextId++, title, description, date, time, advanceMinutes);
        cache.add(reminder);
        flush();
        return reminder;
    }

    /** Reemplaza un recordatorio existente (mismo id) y persiste. */
    public synchronized void update(Reminder reminder) {
        for (int i = 0; i < cache.size(); i++) {
            if (cache.get(i).getId() == reminder.getId()) {
                cache.set(i, reminder);
                flush();
                return;
            }
        }
    }

    /** Elimina por id; devuelve true si existia. */
    public synchronized boolean deleteById(long id) {
        boolean removed = cache.removeIf(r -> r.getId() == id);
        if (removed) {
            flush();
        }
        return removed;
    }

    /** Elimina todos los recordatorios. */
    public synchronized void deleteAll() {
        cache.clear();
        flush();
    }

    public synchronized int count() {
        return cache.size();
    }

    // ----- Persistencia -----

    private void load() {
        cache.clear();
        if (!Files.exists(path)) {
            return;
        }
        try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            long maxId = 0;
            for (String line : lines) {
                if (line.isBlank()) {
                    continue;
                }
                Reminder r = parse(line);
                if (r != null) {
                    cache.add(r);
                    maxId = Math.max(maxId, r.getId());
                }
            }
            nextId = maxId + 1;
            LOGGER.log(Level.INFO, "Recordatorios cargados: {0}", cache.size());
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "No se pudieron leer los recordatorios", ex);
        }
    }

    private void flush() {
        List<String> lines = new ArrayList<>(cache.size());
        for (Reminder r : cache) {
            lines.add(serialize(r));
        }
        try {
            Files.write(path, lines, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "No se pudieron guardar los recordatorios", ex);
        }
    }

    private String serialize(Reminder r) {
        return String.join(FIELD_SEP,
                Long.toString(r.getId()),
                encode(r.getTitle()),
                encode(r.getDescription()),
                r.getDate() == null ? "" : r.getDate().format(DATE_FMT),
                r.getTime() == null ? "" : r.getTime().format(TIME_FMT),
                Integer.toString(r.getAdvanceMinutes()),
                Boolean.toString(r.isNotified()));
    }

    private Reminder parse(String line) {
        String[] parts = line.split("\\" + FIELD_SEP, -1);
        if (parts.length < FIELD_COUNT) {
            LOGGER.log(Level.WARNING, "Linea ignorada (formato invalido): {0}", line);
            return null;
        }
        try {
            long id = Long.parseLong(parts[0]);
            String title = decode(parts[1]);
            String description = decode(parts[2]);
            LocalDate date = parts[3].isEmpty() ? null : LocalDate.parse(parts[3], DATE_FMT);
            LocalTime time = parts[4].isEmpty() ? null : LocalTime.parse(parts[4], TIME_FMT);
            int advance = Integer.parseInt(parts[5]);
            boolean notified = Boolean.parseBoolean(parts[6]);
            Reminder r = new Reminder(id, title, description, date, time, advance);
            r.setNotified(notified);
            return r;
        } catch (RuntimeException ex) {
            LOGGER.log(Level.WARNING, "Linea ignorada (error de parseo): " + line, ex);
            return null;
        }
    }

    private String encode(String value) {
        String safe = value == null ? "" : value;
        return Base64.getEncoder().encodeToString(safe.getBytes(StandardCharsets.UTF_8));
    }

    private String decode(String stored) {
        if (stored.isEmpty()) {
            return "";
        }
        return new String(Base64.getDecoder().decode(stored), StandardCharsets.UTF_8);
    }

    /** Utilidad de solo lectura para pruebas/diagnostico. */
    public List<Reminder> snapshot() {
        return Collections.unmodifiableList(new ArrayList<>(cache));
    }
}
