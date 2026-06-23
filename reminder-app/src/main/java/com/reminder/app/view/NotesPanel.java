package com.reminder.app.view;

import com.reminder.app.config.Theme;
import com.reminder.app.model.Note;
import com.reminder.app.repository.NoteRepository;
import com.reminder.app.util.NoteCrypto;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;

/**
 * Panel de notas tipo "Apple Notes" embebido en la ventana principal.
 *
 * Lista a la izquierda y editor a la derecha; cada nota puede bloquearse con
 * contraseña (contenido cifrado con AES). Comparte la paleta del resto de la app.
 *
 * @author Jesus Gutierrez
 */
public class NotesPanel extends JPanel {

    private final NoteRepository repository;
    private final Runnable onBack;
    private final DefaultListModel<Note> listModel = new DefaultListModel<>();
    private final JList<Note> list = new JList<>(listModel);
    private final JTextField titleField = new JTextField();
    private final JTextArea contentArea = new JTextArea();
    private final JButton lockButton = new JButton("Bloquear");

    private Note current;
    private String sessionPassword;

    public NotesPanel(NoteRepository repository, Runnable onBack) {
        this.repository = repository;
        this.onBack = onBack;
        buildUi();
        refreshList();
    }

    private void buildUi() {
        setLayout(new BorderLayout());
        setBackground(Theme.BACKGROUND);
        setBorder(new EmptyBorder(12, 12, 12, 12));

        // Cabecera: volver + titulo de seccion.
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        header.setOpaque(false);
        JButton back = plain(new JButton("← Recordatorios"), e -> onBack.run());
        JLabel h = new JLabel("Notas");
        h.setFont(Theme.fontBold(18));
        h.setForeground(Theme.TEXT);
        header.add(back);
        header.add(h);
        add(header, BorderLayout.NORTH);

        // Izquierda: lista + acciones.
        JPanel left = new JPanel(new BorderLayout());
        left.setBackground(Theme.SURFACE);
        left.setBorder(new EmptyBorder(8, 8, 8, 8));

        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setBackground(Theme.SURFACE);
        list.setFixedCellHeight(46);
        list.setCellRenderer(new NoteCell());
        list.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                openSelected();
            }
        });
        JScrollPane listScroll = new JScrollPane(list);
        listScroll.setBorder(BorderFactory.createLineBorder(Theme.LINE));
        left.add(listScroll, BorderLayout.CENTER);

        JPanel leftButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 8));
        leftButtons.setOpaque(false);
        leftButtons.add(primary(new JButton("Nueva"), e -> newNote()));
        leftButtons.add(plain(new JButton("Eliminar"), e -> deleteNote()));
        left.add(leftButtons, BorderLayout.SOUTH);

        // Derecha: editor.
        JPanel right = new JPanel(new BorderLayout(0, 10));
        right.setBackground(Theme.BACKGROUND);
        right.setBorder(new EmptyBorder(0, 12, 0, 0));

        titleField.setFont(Theme.fontBold(18));
        titleField.setForeground(Theme.TEXT);
        titleField.putClientProperty("JTextField.placeholderText", "Título de la nota");
        right.add(titleField, BorderLayout.NORTH);

        contentArea.setFont(Theme.fontRegular(14));
        contentArea.setForeground(Theme.TEXT);
        contentArea.setLineWrap(true);
        contentArea.setWrapStyleWord(true);
        contentArea.setBorder(new EmptyBorder(8, 8, 8, 8));
        JScrollPane contentScroll = new JScrollPane(contentArea);
        contentScroll.setBorder(BorderFactory.createLineBorder(Theme.LINE));
        right.add(contentScroll, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(plain(lockButton, e -> toggleLock()));
        actions.add(primary(new JButton("Guardar"), e -> saveCurrent()));
        right.add(actions, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        split.setDividerLocation(220);
        split.setBorder(null);
        split.setOpaque(false);
        add(split, BorderLayout.CENTER);

        setEditorEnabled(false);
    }

    private void newNote() {
        Note n = repository.add("Nueva nota", "");
        refreshList();
        list.setSelectedValue(n, true);
    }

    private void deleteNote() {
        if (current == null) {
            return;
        }
        int ok = JOptionPane.showConfirmDialog(this, "¿Eliminar esta nota?", "Confirmar",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok == JOptionPane.YES_OPTION) {
            repository.deleteById(current.getId());
            current = null;
            sessionPassword = null;
            refreshList();
            clearEditor();
            setEditorEnabled(false);
        }
    }

    private void openSelected() {
        Note sel = list.getSelectedValue();
        if (sel == null) {
            return;
        }
        sessionPassword = null;
        if (sel.isLocked()) {
            String pwd = askPassword("Esta nota está bloqueada. Contraseña:");
            if (pwd == null || !NoteCrypto.hash(pwd).equals(sel.getPasswordHash())) {
                if (pwd != null) {
                    JOptionPane.showMessageDialog(this, "Contraseña incorrecta", "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
                list.clearSelection();
                clearEditor();
                setEditorEnabled(false);
                return;
            }
            try {
                sel.setContent(NoteCrypto.decrypt(sel.getCipher(), pwd));
                sessionPassword = pwd;
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "No se pudo descifrar la nota", "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
        }
        current = sel;
        titleField.setText(sel.getTitle());
        contentArea.setText(sel.getContent());
        contentArea.setCaretPosition(0);
        setEditorEnabled(true);
        lockButton.setText(sel.isLocked() ? "Quitar contraseña" : "Bloquear");
    }

    private void saveCurrent() {
        if (current == null) {
            return;
        }
        String t = titleField.getText().trim();
        current.setTitle(t.isEmpty() ? "Sin título" : t);
        current.setContent(contentArea.getText());
        if (current.isLocked() && sessionPassword != null) {
            try {
                current.setCipher(NoteCrypto.encrypt(current.getContent(), sessionPassword));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "No se pudo cifrar la nota", "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
        }
        repository.update(current);
        refreshList();
        list.setSelectedValue(current, true);
    }

    private void toggleLock() {
        if (current == null) {
            return;
        }
        if (current.isLocked()) {
            current.setLocked(false);
            current.setPasswordHash("");
            current.setCipher("");
            sessionPassword = null;
            repository.update(current);
            lockButton.setText("Bloquear");
        } else {
            String pwd = askPassword("Nueva contraseña para esta nota:");
            if (pwd == null || pwd.isEmpty()) {
                return;
            }
            if (!pwd.equals(askPassword("Repite la contraseña:"))) {
                JOptionPane.showMessageDialog(this, "Las contraseñas no coinciden", "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                current.setContent(contentArea.getText());
                current.setCipher(NoteCrypto.encrypt(current.getContent(), pwd));
                current.setPasswordHash(NoteCrypto.hash(pwd));
                current.setLocked(true);
                sessionPassword = pwd;
                repository.update(current);
                lockButton.setText("Quitar contraseña");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "No se pudo cifrar", "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
        refreshList();
    }

    private String askPassword(String message) {
        JPasswordField field = new JPasswordField();
        int ok = JOptionPane.showConfirmDialog(this, field, message,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        return ok == JOptionPane.OK_OPTION ? new String(field.getPassword()) : null;
    }

    private void refreshList() {
        Note keep = current;
        listModel.clear();
        for (Note n : repository.findAll()) {
            listModel.addElement(n);
        }
        if (keep != null) {
            list.setSelectedValue(keep, true);
        }
    }

    private void clearEditor() {
        titleField.setText("");
        contentArea.setText("");
    }

    private void setEditorEnabled(boolean enabled) {
        titleField.setEnabled(enabled);
        contentArea.setEnabled(enabled);
        lockButton.setEnabled(enabled);
    }

    private JButton primary(JButton b, java.awt.event.ActionListener a) {
        b.setBackground(Theme.PRIMARY);
        b.setForeground(Theme.TEXT_ON_PRIMARY);
        b.setFont(Theme.fontBold(13));
        b.setFocusPainted(false);
        b.addActionListener(a);
        return b;
    }

    private JButton plain(JButton b, java.awt.event.ActionListener a) {
        b.setFont(Theme.fontBold(13));
        b.setFocusPainted(false);
        b.addActionListener(a);
        return b;
    }

    private static class NoteCell extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> l, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
            JLabel c = (JLabel) super.getListCellRendererComponent(l, value, index, isSelected, cellHasFocus);
            Note n = (Note) value;
            String title = n.getTitle().isEmpty() ? "Sin título" : n.getTitle();
            c.setText(n.isLocked() ? title + "   (bloqueada)" : title);
            c.setBorder(new EmptyBorder(6, 12, 6, 12));
            c.setFont(Theme.fontBold(14));
            c.setForeground(Theme.TEXT);
            c.setBackground(isSelected ? Theme.SELECTION : Theme.SURFACE);
            return c;
        }
    }
}
