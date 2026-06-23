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
    private final JPanel attachmentStrip = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 4));

    private Note current;
    private String sessionPassword;

    private static final String ASSETS_DIR = "notes-assets";

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

        attachmentStrip.setOpaque(false);

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        toolbar.setOpaque(false);
        toolbar.add(plain(new JButton("Imagen…"), e -> addAttachment(true)));
        toolbar.add(plain(new JButton("Audio…"), e -> addAttachment(false)));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(plain(lockButton, e -> toggleLock()));
        actions.add(primary(new JButton("Guardar"), e -> saveCurrent()));

        JPanel south = new JPanel(new BorderLayout());
        south.setOpaque(false);
        JPanel bars = new JPanel(new BorderLayout());
        bars.setOpaque(false);
        bars.add(toolbar, BorderLayout.WEST);
        bars.add(actions, BorderLayout.EAST);
        south.add(attachmentStrip, BorderLayout.NORTH);
        south.add(bars, BorderLayout.SOUTH);
        right.add(south, BorderLayout.SOUTH);

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
        refreshAttachments();
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

    /** Copia un archivo (imagen/audio) a la carpeta de la nota y lo adjunta. */
    private void addAttachment(boolean image) {
        if (current == null) {
            return;
        }
        javax.swing.JFileChooser fc = new javax.swing.JFileChooser();
        fc.setFileFilter(image
                ? new javax.swing.filechooser.FileNameExtensionFilter("Imágenes", "png", "jpg", "jpeg", "gif")
                : new javax.swing.filechooser.FileNameExtensionFilter("Audio", "wav", "mp3", "m4a", "ogg"));
        if (fc.showOpenDialog(this) != javax.swing.JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            java.io.File src = fc.getSelectedFile();
            java.nio.file.Path dir = java.nio.file.Paths.get(ASSETS_DIR, String.valueOf(current.getId()));
            java.nio.file.Files.createDirectories(dir);
            java.nio.file.Path dest = dir.resolve(src.getName());
            java.nio.file.Files.copy(src.toPath(), dest,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            current.getAttachments().add(dest.toString());
            repository.update(current);
            refreshAttachments();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "No se pudo adjuntar: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Reconstruye la franja de adjuntos (miniaturas de imagen / botones de audio). */
    private void refreshAttachments() {
        attachmentStrip.removeAll();
        if (current != null) {
            for (String path : current.getAttachments()) {
                java.io.File f = new java.io.File(path);
                String lower = f.getName().toLowerCase();
                boolean isImg = lower.endsWith(".png") || lower.endsWith(".jpg")
                        || lower.endsWith(".jpeg") || lower.endsWith(".gif");
                if (isImg && f.isFile()) {
                    javax.swing.ImageIcon icon = new javax.swing.ImageIcon(
                            new javax.swing.ImageIcon(path).getImage()
                                    .getScaledInstance(72, 72, java.awt.Image.SCALE_SMOOTH));
                    JLabel thumb = new JLabel(icon);
                    thumb.setToolTipText(f.getName() + " (clic para abrir)");
                    thumb.setBorder(BorderFactory.createLineBorder(Theme.LINE));
                    thumb.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
                    thumb.addMouseListener(new java.awt.event.MouseAdapter() {
                        @Override
                        public void mouseClicked(java.awt.event.MouseEvent e) {
                            openFile(f);
                        }
                    });
                    attachmentStrip.add(thumb);
                } else {
                    JButton play = plain(new JButton("♪ " + f.getName()), e -> openFile(f));
                    attachmentStrip.add(play);
                }
            }
        }
        attachmentStrip.revalidate();
        attachmentStrip.repaint();
    }

    private void openFile(java.io.File f) {
        try {
            if (f.isFile() && java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().open(f);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "No se pudo abrir el archivo", "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
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
        refreshAttachments();
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
