package com.reminder.app.service;

import com.reminder.app.model.Reminder;
import com.reminder.app.repository.ReminderRepository;
import java.time.LocalDateTime;
import java.util.function.Consumer;
import javax.swing.Timer;

/**
 * Planificador de avisos.
 *
 * Revisa periodicamente el repositorio y dispara un aviso por cada recordatorio
 * cuya hora de notificacion ({@link Reminder#notifyAt()}) ya llego y que aun no
 * ha sido avisado. Usa un {@link javax.swing.Timer}, por lo que el callback se
 * ejecuta en el Event Dispatch Thread y puede tocar la UI con seguridad.
 *
 * @author Jesus Gutierrez
 */
public class ReminderScheduler {

    /** Intervalo de comprobacion (ms). */
    private static final int CHECK_INTERVAL_MS = 15_000;

    private final ReminderRepository repository;
    private final Consumer<Reminder> onDue;
    private final Timer timer;

    public ReminderScheduler(ReminderRepository repository, Consumer<Reminder> onDue) {
        this.repository = repository;
        this.onDue = onDue;
        this.timer = new Timer(CHECK_INTERVAL_MS, e -> check());
        this.timer.setInitialDelay(0); // primera revision inmediata
    }

    public void start() {
        timer.start();
    }

    public void stop() {
        timer.stop();
    }

    /** Fuerza una revision inmediata (util tras crear un recordatorio). */
    public void checkNow() {
        check();
    }

    private void check() {
        LocalDateTime now = LocalDateTime.now();
        for (Reminder r : repository.findAll()) {
            if (!r.isNotified() && !r.notifyAt().isAfter(now)) {
                r.setNotified(true);
                repository.update(r);
                onDue.accept(r);
            }
        }
    }
}
