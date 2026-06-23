package com.reminder.app.view;

import com.reminder.app.config.Theme;
import com.reminder.app.model.Note;
import com.reminder.app.repository.NoteRepository;
import com.reminder.app.util.NoteCrypto;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
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
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

/**
 * Panel de notas tipo "Apple Notes" con editor enriquecido.
 *
 * El contenido es HTML (JTextPane + HTMLEditorKit): permite escribir, insertar
 * imagenes en linea, dibujar (se inserta como imagen) y adjuntar audio como
 * enlace reproducible. Tambien se pueden arrastrar archivos al editor. Cada nota
 * puede bloquearse con contraseña (contenido cifrado con AES).
 *
 * @author Jesus Gutierrez
 */
public class NotesPanel extends JPanel {

    private static final String ASSETS_DIR = "notes-assets";

    private final NoteRepository repository;
    private final Runnable onBack;
    private final DefaultListModel<Note> listModel = new DefaultListModel<>();
    private final JList<Note> list = new JList<>(listModel);
    private final JTextField titleField = new JTextField();
    private final JTextPane editor = new JTextPane();
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

        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        header.setOpaque(false);
        header.add(plain(new JButton("← Recordatorios"), e -> onBack.run()));
        JLabel h = new JLabel("Notas");
        h.setFont(Theme.fontBold(18));
        h.setForeground(Theme.TEXT);
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

        // Derecha: editor enriquecido.
        JPanel right = new JPanel(new BorderLayout(0, 8));
        right.setBackground(Theme.BACKGROUND);
        right.setBorder(new EmptyBorder(0, 12, 0, 0));

        titleField.setFont(Theme.fontBold(18));
        titleField.setForeground(Theme.TEXT);
        titleField.putClientProperty("JTextField.placeholderText", "Título de la nota");
        right.add(titleField, BorderLayout.NORTH);

        editor.setEditorKit(new HTMLEditorKit());
        editor.setContentType("text/html");
        editor.setFont(Theme.fontRegular(14));
        editor.setBorder(new EmptyBorder(8, 8, 8, 8));
        editor.addHyperlinkListener(e -> {
            if (e.getEventType() == javax.swing.event.HyperlinkEvent.EventType.ACTIVATED
                    && e.getURL() != null) {
                openUrl(e.getURL());
            }
        });
        enableFileDrop(editor);
        JScrollPane editorScroll = new JScrollPane(editor);
        editorScroll.setBorder(BorderFactory.createLineBorder(Theme.LINE));
        right.add(editorScroll, BorderLayout.CENTER);

        // Barra de herramientas del editor + acciones.
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        toolbar.setOpaque(false);
        JButton bold = plain(new JButton("N"), null);
        bold.setFont(Theme.fontBold(14));
        bold.addActionListener(new javax.swing.text.StyledEditorKit.BoldAction());
        toolbar.add(bold);
        toolbar.add(plain(new JButton("Imagen"), e -> insertImage()));
        toolbar.add(plain(new JButton("Dibujar"), e -> insertDrawing()));
        toolbar.add(plain(new JButton("Audio"), e -> insertAudio()));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(plain(lockButton, e -> toggleLock()));
        actions.add(primary(new JButton("Guardar"), e -> saveCurrent()));

        JPanel south = new JPanel(new BorderLayout());
        south.setOpaque(false);
        south.add(toolbar, BorderLayout.WEST);
        south.add(actions, BorderLayout.EAST);
        right.add(south, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        split.setDividerLocation(220);
        split.setBorder(null);
        split.setOpaque(false);
        add(split, BorderLayout.CENTER);

        setEditorEnabled(false);
    }

    // ----- Lista / CRUD -----

    private void newNote() {
        Note n = repository.add("Nueva nota", "");
        refreshList();
        list.setSelectedValue(n, true);
    }

    private void deleteNote() {
        if (current == null) {
            return;
        }
        if (JOptionPane.showConfirmDialog(this, "¿Eliminar esta nota?", "Confirmar",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
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
        editor.setText(sel.getContent() == null || sel.getContent().isEmpty()
                ? "<html><body></body></html>" : sel.getContent());
        editor.setCaretPosition(0);
        setEditorEnabled(true);
        lockButton.setText(sel.isLocked() ? "Quitar contraseña" : "Bloquear");
    }

    private void saveCurrent() {
        if (current == null) {
            return;
        }
        String t = titleField.getText().trim();
        current.setTitle(t.isEmpty() ? "Sin título" : t);
        current.setContent(editor.getText());
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

    // ----- Contenido enriquecido -----

    private void insertImage() {
        if (!ensureNote()) {
            return;
        }
        javax.swing.JFileChooser fc = new javax.swing.JFileChooser();
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Imágenes", "png", "jpg", "jpeg", "gif"));
        if (fc.showOpenDialog(this) == javax.swing.JFileChooser.APPROVE_OPTION) {
            Path dest = copyToAssets(fc.getSelectedFile());
            if (dest != null) {
                insertImageHtml(dest);
            }
        }
    }

    private void insertDrawing() {
        if (!ensureNote()) {
            return;
        }
        BufferedImage img = new SketchDialog(javax.swing.SwingUtilities.getWindowAncestor(this)).showAndGet();
        if (img == null) {
            return;
        }
        try {
            Path dir = Paths.get(ASSETS_DIR, String.valueOf(current.getId()));
            Files.createDirectories(dir);
            Path dest = dir.resolve("dibujo-" + System.identityHashCode(img) + ".png");
            ImageIO.write(img, "png", dest.toFile());
            current.getAttachments().add(dest.toString());
            insertImageHtml(dest);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "No se pudo guardar el dibujo", "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void insertAudio() {
        if (!ensureNote()) {
            return;
        }
        javax.swing.JFileChooser fc = new javax.swing.JFileChooser();
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Audio", "wav", "mp3", "m4a", "ogg"));
        if (fc.showOpenDialog(this) == javax.swing.JFileChooser.APPROVE_OPTION) {
            Path dest = copyToAssets(fc.getSelectedFile());
            if (dest != null) {
                insertAudioHtml(dest);
            }
        }
    }

    private void insertImageHtml(Path dest) {
        try {
            String url = dest.toUri().toURL().toString();
            insertHtmlAtCaret("<img src='" + url + "' width='320'><br>", HTML.Tag.IMG);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "No se pudo insertar la imagen", "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void insertAudioHtml(Path dest) {
        try {
            String url = dest.toUri().toURL().toString();
            insertHtmlAtCaret("<p>&#9654; <a href='" + url + "'>"
                    + esc(dest.getFileName().toString()) + "</a></p>", HTML.Tag.A);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "No se pudo insertar el audio", "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void insertHtmlAtCaret(String html, HTML.Tag tag) throws Exception {
        HTMLDocument doc = (HTMLDocument) editor.getDocument();
        HTMLEditorKit kit = (HTMLEditorKit) editor.getEditorKit();
        int pos = editor.getCaretPosition();
        kit.insertHTML(doc, pos, html, 0, 0, tag);
    }

    /** Permite arrastrar archivos al editor: imagen -> en linea; audio -> enlace. */
    @SuppressWarnings("unchecked")
    private void enableFileDrop(Component comp) {
        new java.awt.dnd.DropTarget(comp, new java.awt.dnd.DropTargetAdapter() {
            @Override
            public void drop(java.awt.dnd.DropTargetDropEvent ev) {
                try {
                    ev.acceptDrop(java.awt.dnd.DnDConstants.ACTION_COPY);
                    java.util.List<File> files = (java.util.List<File>) ev.getTransferable()
                            .getTransferData(java.awt.datatransfer.DataFlavor.javaFileListFlavor);
                    if (!ensureNote()) {
                        return;
                    }
                    for (File f : files) {
                        Path dest = copyToAssets(f);
                        if (dest == null) {
                            continue;
                        }
                        if (isImage(f.getName())) {
                            insertImageHtml(dest);
                        } else {
                            insertAudioHtml(dest);
                        }
                    }
                } catch (Exception ignore) {
                    // drop invalido
                }
            }
        });
    }

    private Path copyToAssets(File src) {
        if (src == null || !src.isFile()) {
            return null;
        }
        try {
            Path dir = Paths.get(ASSETS_DIR, String.valueOf(current.getId()));
            Files.createDirectories(dir);
            Path dest = dir.resolve(src.getName());
            Files.copy(src.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
            current.getAttachments().add(dest.toString());
            return dest;
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "No se pudo copiar el archivo", "Error",
                    JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    private boolean isImage(String name) {
        String n = name.toLowerCase();
        return n.endsWith(".png") || n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".gif");
    }

    private boolean ensureNote() {
        if (current == null) {
            JOptionPane.showMessageDialog(this, "Crea o selecciona una nota primero", "Notas",
                    JOptionPane.INFORMATION_MESSAGE);
            return false;
        }
        return true;
    }

    private void openUrl(java.net.URL url) {
        try {
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().open(new File(url.toURI()));
            }
        } catch (Exception ex) {
            // no se pudo abrir
        }
    }

    // ----- Bloqueo -----

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
                current.setContent(editor.getText());
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

    // ----- Helpers -----

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
        editor.setText("<html><body></body></html>");
    }

    private void setEditorEnabled(boolean enabled) {
        titleField.setEnabled(enabled);
        editor.setEnabled(enabled);
        lockButton.setEnabled(enabled);
    }

    private String esc(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private JButton primary(JButton b, java.awt.event.ActionListener a) {
        b.setBackground(Theme.PRIMARY);
        b.setForeground(Theme.TEXT_ON_PRIMARY);
        b.setFont(Theme.fontBold(13));
        b.setFocusPainted(false);
        if (a != null) {
            b.addActionListener(a);
        }
        return b;
    }

    private JButton plain(JButton b, java.awt.event.ActionListener a) {
        b.setFont(Theme.fontBold(13));
        b.setFocusPainted(false);
        if (a != null) {
            b.addActionListener(a);
        }
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
