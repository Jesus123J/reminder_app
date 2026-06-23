package com.reminder.app.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

/**
 * Entidad de dominio que representa un recordatorio.
 *
 * Es un POJO inmutable en su identidad ({@code id}) y editable en el resto de
 * campos. Modela el "que" (titulo, descripcion), el "cuando" (fecha + hora) y
 * la antelacion con la que se debe avisar.
 *
 * @author Jesus Gutierrez
 */
public class Reminder {

    private final long id;
    private String title;
    private String description;
    private LocalDate date;
    private LocalTime time;
    /** Minutos de antelacion con que se notifica antes de la hora objetivo. */
    private int advanceMinutes;
    /** Marca si ya se disparo la notificacion (evita avisos repetidos). */
    private boolean notified;

    public Reminder(long id, String title, String description,
                    LocalDate date, LocalTime time, int advanceMinutes) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.date = date;
        this.time = time;
        this.advanceMinutes = advanceMinutes;
        this.notified = false;
    }

    /** Instante exacto en que vence el recordatorio (fecha + hora). */
    public LocalDateTime dueAt() {
        LocalDate d = date != null ? date : LocalDate.now();
        LocalTime t = time != null ? time : LocalTime.MIDNIGHT;
        return LocalDateTime.of(d, t);
    }

    /** Instante en que debe dispararse la notificacion (vencimiento - antelacion). */
    public LocalDateTime notifyAt() {
        return dueAt().minusMinutes(advanceMinutes);
    }

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public LocalTime getTime() {
        return time;
    }

    public void setTime(LocalTime time) {
        this.time = time;
    }

    public int getAdvanceMinutes() {
        return advanceMinutes;
    }

    public void setAdvanceMinutes(int advanceMinutes) {
        this.advanceMinutes = advanceMinutes;
    }

    public boolean isNotified() {
        return notified;
    }

    public void setNotified(boolean notified) {
        this.notified = notified;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Reminder)) {
            return false;
        }
        return id == ((Reminder) o).id;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Reminder{id=" + id + ", title='" + title + "', dueAt=" + dueAt() + '}';
    }
}
