package com.reminder.app.controller;

import com.reminder.app.model.ModelReminderData;
import com.reminder.app.model.Reminder;
import com.reminder.app.repository.ReminderRepository;
import com.reminder.app.service.ReminderScheduler;
import com.reminder.app.service.TrayNotifier;
import com.reminder.app.service.integration.GoogleCalendarIntegration;
import com.reminder.app.service.integration.GoogleCalendarLinkIntegration;
import com.reminder.app.service.integration.IcsExportIntegration;
import com.reminder.app.service.integration.IntegrationManager;
import com.reminder.app.view.ViewReminder;
import com.reminder.app.view.components.Action_button;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import javax.swing.JOptionPane;

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
    private final IntegrationManager integrations;

    /** Lista actual mostrada en la tabla; su orden coincide con las filas. */
    private List<Reminder> currentReminders;

    public ControllerReminder() {
        this.repository = new ReminderRepository();
        this.trayNotifier = new TrayNotifier();
        this.integrations = new IntegrationManager();
        registerIntegrations();
        this.viewReminder = new ViewReminder(this);
        super.init(viewReminder);
        viewReminder.buttonSaveData.addActionListener(this);
        viewReminder.getDeleteAllButton().addActionListener(e -> deleteAll());
        viewReminder.installIntegrationsMenu(integrations);
        refreshTable();

        // Arranca el planificador de avisos (revisa cada 30s, primer chequeo inmediato).
        this.scheduler = new ReminderScheduler(repository, this::onReminderDue);
        this.scheduler.start();
    }

    /**
     * Registra las integraciones externas disponibles. Para anadir una nueva
     * conexion (Google Calendar, Outlook, email, webhook...) basta con
     * implementar {@link com.reminder.app.service.integration.ReminderIntegration}
     * y registrarla aqui.
     */
    private void registerIntegrations() {
        // Sin token: el usuario las activa desde el menu "Integraciones".
        integrations.register(new GoogleCalendarLinkIntegration());
        integrations.register(new IcsExportIntegration());
        // Avanzada (OAuth): se habilita sola si existe client_secret.json.
        integrations.register(new GoogleCalendarIntegration());
        // Para anadir mas conexiones: integrations.register(new TuIntegracion());
    }

    /** Acceso al gestor de integraciones (para la UI de extensiones). */
    public IntegrationManager getIntegrations() {
        return integrations;
    }

    /** Se invoca (en el EDT) cuando un recordatorio alcanza su hora de aviso. */
    private void onReminderDue(Reminder r) {
        String body = (r.getDescription() == null || r.getDescription().isBlank())
                ? "Vence a las " + r.getTime()
                : r.getDescription();
        // Notificacion NATIVA de Windows (bandeja) + sonido. Es la que aparece
        // en el sistema aunque la ventana no tenga el foco.
        trayNotifier.show("Recordatorio: " + r.getTitle(), body);
        java.awt.Toolkit.getDefaultToolkit().beep();
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

        Reminder saved = repository.add(title, description, date, time, advance);
        integrations.dispatchCreated(saved);
        viewReminder.clearForm();
        refreshTable();
        // Revision inmediata: si ya vencio (segun la antelacion), avisa al instante.
        scheduler.checkNow();
        trayNotifier.show("Recordatorio guardado", title);
    }

    private void warn(String message) {
        JOptionPane.showMessageDialog(viewReminder, message, "Atención",
                JOptionPane.WARNING_MESSAGE);
    }

    @Override
    public void delete_index(int index) {
        if (currentReminders == null || index < 0 || index >= currentReminders.size()) {
            return;
        }
        Reminder target = currentReminders.get(index);
        repository.deleteById(target.getId());
        integrations.dispatchDeleted(target);
        refreshTable();
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
        }
    }
}
