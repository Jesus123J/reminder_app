package com.reminder.app.controller;

import com.reminder.app.model.ModelReminderData;
import com.reminder.app.model.Reminder;
import com.reminder.app.repository.ReminderRepository;
import com.reminder.app.service.ReminderScheduler;
import com.reminder.app.service.TrayNotifier;
import com.reminder.app.view.ViewReminder;
import com.reminder.app.view.components.Action_button;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import javax.swing.JOptionPane;
import raven.toast.Notifications;

/**
 * Controlador principal: coordina la vista con el repositorio de recordatorios.
 *
 * @author Jesus Gutierrez
 */
public class ControllerReminder extends ModelReminderData implements ActionListener, Action_button {

    private final ViewReminder viewReminder;
    private final ReminderRepository repository;
    private final ReminderScheduler scheduler;
    private final TrayNotifier trayNotifier;

    /** Lista actual mostrada en la tabla; su orden coincide con las filas. */
    private List<Reminder> currentReminders;

    public ControllerReminder() {
        this.repository = new ReminderRepository();
        this.trayNotifier = new TrayNotifier();
        this.viewReminder = new ViewReminder(this);
        super.init(viewReminder);
        viewReminder.buttonSaveData.addActionListener(this);
        viewReminder.getDeleteAllButton().addActionListener(e -> deleteAll());
        refreshTable();

        // Arranca el planificador de avisos (revisa cada 30s, primer chequeo inmediato).
        this.scheduler = new ReminderScheduler(repository, this::onReminderDue);
        this.scheduler.start();
    }

    /** Se invoca (en el EDT) cuando un recordatorio alcanza su hora de aviso. */
    private void onReminderDue(Reminder r) {
        String body = (r.getDescription() == null || r.getDescription().isBlank())
                ? "Vence a las " + r.getTime()
                : r.getDescription();
        Notifications.getInstance().show(Notifications.Type.INFO,
                Notifications.Location.BOTTOM_RIGHT, "🔔 " + r.getTitle());
        trayNotifier.show("🔔 " + r.getTitle(), body);
        refreshTable();
    }

    /** Recarga la tabla desde el repositorio. */
    private void refreshTable() {
        currentReminders = repository.findAll();
        viewReminder.loadReminders(currentReminders);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(viewReminder.buttonSaveData)) {
            saveReminder();
        }
    }

    private void saveReminder() {
        String title = viewReminder.getTitleInput();
        String description = viewReminder.getDescriptionInput();
        LocalDate date = viewReminder.getSelectedDate();
        LocalTime time = viewReminder.getSelectedTime();
        int advance = viewReminder.getAdvanceMinutes();

        if (title.isEmpty()) {
            warn("El título es obligatorio");
            return;
        }
        if (date == null || time == null) {
            warn("Selecciona fecha y hora");
            return;
        }

        repository.add(title, description, date, time, advance);
        viewReminder.clearForm();
        refreshTable();
        Notifications.getInstance().show(Notifications.Type.SUCCESS,
                Notifications.Location.BOTTOM_RIGHT, "Recordatorio guardado");
    }

    private void warn(String message) {
        Notifications.getInstance().show(Notifications.Type.WARNING,
                Notifications.Location.BOTTOM_RIGHT, message);
    }

    @Override
    public void delete_index(int index) {
        if (currentReminders == null || index < 0 || index >= currentReminders.size()) {
            return;
        }
        Reminder target = currentReminders.get(index);
        repository.deleteById(target.getId());
        refreshTable();
        Notifications.getInstance().show(Notifications.Type.SUCCESS,
                Notifications.Location.BOTTOM_RIGHT, "Recordatorio eliminado");
    }

    private void deleteAll() {
        if (repository.count() == 0) {
            warn("No hay recordatorios para eliminar");
            return;
        }
        int option = JOptionPane.showConfirmDialog(viewReminder,
                "¿Eliminar todos los recordatorios?", "Confirmar",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (option == JOptionPane.YES_OPTION) {
            repository.deleteAll();
            refreshTable();
            Notifications.getInstance().show(Notifications.Type.SUCCESS,
                    Notifications.Location.BOTTOM_RIGHT, "Se eliminaron todos los recordatorios");
        }
    }
}
