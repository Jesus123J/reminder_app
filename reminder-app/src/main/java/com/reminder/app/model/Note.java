package com.reminder.app.model;

/**
 * Nota tipo "Apple Notes".
 *
 * Puede estar bloqueada con contraseña: en ese caso su contenido se guarda
 * cifrado (campo {@code cipher}) y {@code content} solo existe en memoria tras
 * desbloquearla.
 *
 * @author Jesus Gutierrez
 */
public class Note {

    private final long id;
    private String title;
    private String content;        // texto en claro (cuando esta desbloqueada)
    private boolean locked;
    private String passwordHash;   // SHA-256 de la contraseña (verificacion rapida)
    private String cipher;         // contenido cifrado (cuando esta bloqueada)

    public Note(long id, String title, String content) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.locked = false;
        this.passwordHash = "";
        this.cipher = "";
    }

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title == null ? "" : title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content == null ? "" : content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public String getPasswordHash() {
        return passwordHash == null ? "" : passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash == null ? "" : passwordHash;
    }

    public String getCipher() {
        return cipher == null ? "" : cipher;
    }

    public void setCipher(String cipher) {
        this.cipher = cipher == null ? "" : cipher;
    }
}
