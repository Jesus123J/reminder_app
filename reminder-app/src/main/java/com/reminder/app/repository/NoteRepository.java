package com.reminder.app.repository;

import com.reminder.app.model.Note;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Persistencia de notas en {@code notes.dat} (texto plano, una linea por nota).
 *
 * Formato: id | base64(titulo) | locked | payload | passwordHash
 * - payload = base64(contenido) si NO esta bloqueada; blob cifrado si lo esta.
 *
 * @author Jesus Gutierrez
 */
public class NoteRepository {

    private static final Logger LOGGER = Logger.getLogger(NoteRepository.class.getName());
    private static final String SEP = "|";

    private final Path path;
    private final List<Note> cache = new ArrayList<>();
    private long nextId = 1;

    public NoteRepository() {
        this(Paths.get("notes.dat"));
    }

    public NoteRepository(Path path) {
        this.path = path;
        load();
    }

    public synchronized List<Note> findAll() {
        return new ArrayList<>(cache);
    }

    public synchronized Note add(String title, String content) {
        Note note = new Note(nextId++, title, content);
        cache.add(note);
        flush();
        return note;
    }

    public synchronized void update(Note note) {
        for (int i = 0; i < cache.size(); i++) {
            if (cache.get(i).getId() == note.getId()) {
                cache.set(i, note);
                flush();
                return;
            }
        }
    }

    public synchronized boolean deleteById(long id) {
        boolean removed = cache.removeIf(n -> n.getId() == id);
        if (removed) {
            flush();
        }
        return removed;
    }

    private void load() {
        cache.clear();
        if (!Files.exists(path)) {
            return;
        }
        try {
            long maxId = 0;
            for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                if (line.isBlank()) {
                    continue;
                }
                String[] p = line.split("\\" + SEP, -1);
                if (p.length < 5) {
                    continue;
                }
                long id = Long.parseLong(p[0]);
                Note n = new Note(id, dec(p[1]), "");
                n.setLocked(Boolean.parseBoolean(p[2]));
                if (n.isLocked()) {
                    n.setCipher(p[3]);
                } else {
                    n.setContent(dec(p[3]));
                }
                n.setPasswordHash(p[4]);
                cache.add(n);
                maxId = Math.max(maxId, id);
            }
            nextId = maxId + 1;
            LOGGER.log(Level.INFO, "Notas cargadas: {0}", cache.size());
        } catch (IOException | RuntimeException ex) {
            LOGGER.log(Level.SEVERE, "No se pudieron leer las notas", ex);
        }
    }

    private void flush() {
        List<String> lines = new ArrayList<>();
        for (Note n : cache) {
            String payload = n.isLocked() ? n.getCipher() : enc(n.getContent());
            lines.add(String.join(SEP,
                    Long.toString(n.getId()),
                    enc(n.getTitle()),
                    Boolean.toString(n.isLocked()),
                    payload,
                    n.getPasswordHash()));
        }
        try {
            Files.write(path, lines, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "No se pudieron guardar las notas", ex);
        }
    }

    private String enc(String s) {
        return Base64.getEncoder().encodeToString((s == null ? "" : s).getBytes(StandardCharsets.UTF_8));
    }

    private String dec(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        return new String(Base64.getDecoder().decode(s), StandardCharsets.UTF_8);
    }
}
