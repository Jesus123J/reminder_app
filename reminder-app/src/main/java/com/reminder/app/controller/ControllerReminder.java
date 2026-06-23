package com.reminder.app.controller;

import com.reminder.app.model.ModelReminderData;
import com.reminder.app.model.Reminder;
import com.reminder.app.repository.ReminderRepository;
import com.reminder.app.service.ReminderScheduler;
import com.reminder.app.service.SoundPlayer;
import com.reminder.app.service.TrayNotifier;
import com.reminder.app.service.integration.EmailDraftIntegration;
import com.reminder.app.service.integration.GoogleCalendarIntegration;
import com.reminder.app.service.integration.GoogleCalendarLinkIntegration;
import com.reminder.app.service.integration.IcsExportIntegration;
import com.reminder.app.service.integration.IntegrationManager;
import com.reminder.app.service.integration.OutlookCalendarLinkIntegration;
import com.reminder.app.service.integration.WebhookIntegration;
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
    private final SoundPlayer soundPlayer = new SoundPlayer();
    private final com.reminder.app.repository.NoteRepository noteRepository =
            new com.reminder.app.repository.NoteRepository();
    private com.reminder.app.view.NotesView notesView;

    /** Lista actual mostrada en la tabla; su orden coincide con las filas. */
    private List<Reminder> currentReminders;

    /** Id del recordatorio en edicion; null si se esta creando uno nuevo. */
    private Long editingId;

    /** Estado de busqueda/filtro de la tabla. */
    private String searchText = "";
    private String filterKey = "Todos";

    public ControllerReminder() {
        this.repository = new ReminderRepository();
        this.trayNotifier = new TrayNotifier();
        this.integrations = new IntegrationManager();
        registerIntegrations();
        this.viewReminder = new ViewReminder(this);
        super.init(viewReminder);
        viewReminder.buttonSaveData.addActionListener(this);
        viewReminder.getDeleteAllButton().addActionListener(e -> deleteAll());
        viewReminder.getEditButton().addActionListener(e -> loadSelectedForEdit());
        viewReminder.setRowSelectionHandler(this::loadForEdit);
        viewReminder.installSnoozeMenu(this::snooze);
        viewReminder.installIntegrationsMenu(integrations, soundPlayer, this::applyFilter, this::openNotes);
        trayNotifier.enableMinimizeToTray(viewReminder);
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
        integrations.register(new OutlookCalendarLinkIntegration());
        integrations.register(new EmailDraftIntegration());
        integrations.register(new IcsExportIntegration());
        integrations.register(new WebhookIntegration());
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
        soundPlayer.play();
        refreshTable();
    }

    /** Recarga la tabla desde el repositorio. */
    private void refreshTable() {
        java.time.LocalDate today = java.time.LocalDate.now();
        String q = searchText.toLowerCase();
        java.util.List<Reminder> filtered = new java.util.ArrayList<>();
        for (Reminder r : repository.findAll()) {
            // Filtro de texto (titulo + descripcion + categoria).
            if (!q.isEmpty()) {
                String hay = (safe(r.getTitle()) + " " + safe(r.getDescription())
                        + " " + safe(r.getCategory())).toLowerCase();
                if (!hay.contains(q)) {
                    continue;
                }
            }
            // Filtro por fecha.
            java.time.LocalDate d = r.getDate();
            switch (filterKey) {
                case "Hoy":
                    if (d == null || !d.equals(today)) { continue; }
                    break;
                case "Próximos":
                    if (d == null || d.isBefore(today)) { continue; }
                    break;
                case "Vencidos":
                    if (r.isNotified() || !r.dueAt().isBefore(java.time.LocalDateTime.now())) { continue; }
                    break;
                default:
                    break; // "Todos"
            }
            filtered.add(r);
        }
        currentReminders = filtered;
        viewReminder.loadReminders(currentReminders);
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    /** Abre (o trae al frente) la ventana de notas. */
    private void openNotes() {
        if (notesView == null) {
            notesView = new com.reminder.app.view.NotesView(noteRepository);
        }
        notesView.setVisible(true);
        notesView.toFront();
        notesView.requestFocus();
    }

    /** Actualiza el filtro de busqueda/fecha y refresca. */
    private void applyFilter(String text, String key) {
        this.searchText = text == null ? "" : text;
        this.filterKey = key == null ? "Todos" : key;
        refreshTable();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(viewReminder.buttonSaveData)) {
            saveReminder();
        }
    }

    /** Carga en el formulario el recordatorio de la fila indicada para editarlo. */
    private void loadForEdit(int row) {
        if (currentReminders == null || row < 0 || row >= currentReminders.size()) {
            return;
        }
        Reminder r = currentReminders.get(row);
        editingId = r.getId();
        viewReminder.loadIntoForm(r.getTitle(), r.getDescription(),
                r.getDate(), r.getTime(), r.getAdvanceMinutes(),
                r.getPriority(), r.getCategory(), r.getRecurrence());
    }

    private void loadSelectedForEdit() {
        int row = viewReminder.getSelectedRow();
        if (row < 0) {
            warn("Selecciona una fila para editar");
            return;
        }
        loadForEdit(row);
    }

    private void saveReminder() {
        String title = viewReminder.getTitleInput();
        String description = viewReminder.getDescriptionInput();
        LocalDate date = viewReminder.getSelectedDate();
        LocalTime time = viewReminder.getSelectedTime();
        int advance = viewReminder.getAdvanceMinutes();
        Reminder.Priority priority = viewReminder.getPriority();
        String category = viewReminder.getCategory();
        Reminder.Recurrence recurrence = viewReminder.getRecurrence();

        if (title.isEmpty()) {
            warn("El título es obligatorio");
            return;
        }
        if (date == null || time == null) {
            warn("Selecciona fecha y hora");
            return;
        }

        if (editingId != null) {
            // Actualizar el recordatorio existente.
            Reminder existing = findById(editingId);
            if (existing != null) {
                existing.setTitle(title);
                existing.setDescription(description);
                existing.setDate(date);
                existing.setTime(time);
                existing.setAdvanceMinutes(advance);
                existing.setPriority(priority);
                existing.setCategory(category);
                existing.setRecurrence(recurrence);
                existing.setNotified(false); // permite que vuelva a avisar con los nuevos datos
                repository.update(existing);
            }
            editingId = null;
            viewReminder.clearForm();
            refreshTable();
            scheduler.checkNow();
            trayNotifier.show("Recordatorio actualizado", title);
            return;
        }

        Reminder saved = repository.add(title, description, date, time, advance, priority, category);
        saved.setRecurrence(recurrence);
        repository.update(saved);
        integrations.dispatchCreated(saved);
        viewReminder.clearForm();
        refreshTable();
        // Revision inmediata: si ya vencio (segun la antelacion), avisa al instante.
        scheduler.checkNow();
        trayNotifier.show("Recordatorio guardado", title);
    }

    /** Pospone el recordatorio de la fila: lo reprograma a ahora + minutos. */
    private void snooze(int row, int minutes) {
        if (currentReminders == null || row < 0 || row >= currentReminders.size()) {
            return;
        }
        Reminder r = currentReminders.get(row);
        java.time.LocalDateTime newDue = java.time.LocalDateTime.now().plusMinutes(minutes);
        r.setDate(newDue.toLocalDate());
        r.setTime(newDue.toLocalTime().withSecond(0).withNano(0));
        r.setAdvanceMinutes(0);
        r.setNotified(false);
        repository.update(r);
        refreshTable();
        trayNotifier.show("Pospuesto", r.getTitle() + " · en " + minutes + " min");
    }

    private Reminder findById(long id) {
        for (Reminder r : repository.findAll()) {
            if (r.getId() == id) {
                return r;
            }
        }
        return null;
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
