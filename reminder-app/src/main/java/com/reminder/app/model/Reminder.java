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

    /** Nivel de prioridad del recordatorio. */
    public enum Priority { ALTA, MEDIA, BAJA }

    /** Periodicidad de repeticion. */
    public enum Recurrence { NINGUNA, DIARIA, SEMANAL, MENSUAL }

    private final long id;
    private String title;
    private String description;
    private LocalDate date;
    private LocalTime time;
    /** Minutos de antelacion con que se notifica antes de la hora objetivo. */
    private int advanceMinutes;
    /** Marca si ya se disparo la notificacion (evita avisos repetidos). */
    private boolean notified;
    /** Prioridad (por defecto MEDIA). */
    private Priority priority = Priority.MEDIA;
    /** Categoria/etiqueta libre (por defecto vacia). */
    private String category = "";
    /** Periodicidad (por defecto sin repeticion). */
    private Recurrence recurrence = Recurrence.NINGUNA;

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

    public Priority getPriority() {
        return priority == null ? Priority.MEDIA : priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority == null ? Priority.MEDIA : priority;
    }

    public String getCategory() {
        return category == null ? "" : category;
    }

    public void setCategory(String category) {
        this.category = category == null ? "" : category;
    }

    public Recurrence getRecurrence() {
        return recurrence == null ? Recurrence.NINGUNA : recurrence;
    }

    public void setRecurrence(Recurrence recurrence) {
        this.recurrence = recurrence == null ? Recurrence.NINGUNA : recurrence;
    }

    /** ¿Se repite? */
    public boolean isRecurring() {
        return getRecurrence() != Recurrence.NINGUNA;
    }

    /**
     * Calcula la proxima fecha (segun la periodicidad) cuyo vencimiento sea
     * posterior a {@code now}. Si no se repite, devuelve la fecha actual.
     */
    public LocalDate nextDateAfter(LocalDateTime now) {
        if (!isRecurring() || date == null) {
            return date;
        }
        LocalDate d = date;
        LocalTime t = time != null ? time : LocalTime.MIDNIGHT;
        int guard = 0; // evita bucles infinitos ante datos raros
        while (!LocalDateTime.of(d, t).isAfter(now) && guard++ < 5000) {
            switch (getRecurrence()) {
                case DIARIA:  d = d.plusDays(1); break;
                case SEMANAL: d = d.plusWeeks(1); break;
                case MENSUAL: d = d.plusMonths(1); break;
                default: return d;
            }
        }
        return d;
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
