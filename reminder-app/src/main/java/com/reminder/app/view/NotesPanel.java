package com.reminder.app.view;

import com.reminder.app.config.Theme;
import com.reminder.app.model.Note;
import com.reminder.app.repository.NoteRepository;
import com.reminder.app.util.NoteCrypto;
import java.awt.BorderLayout;
import java.awt.Component;
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
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;

/**
 * Panel de notas con **pizarra libre** ({@link FreeBoardPanel}).
 *
 * Cada nota es un lienzo donde se colocan cajas de texto, imagenes y audio que
 * se mueven y redimensionan libremente. Se puede bloquear con contraseña
 * (contenido cifrado con AES).
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
    private final FreeBoardPanel board = new FreeBoardPanel();
    private final JButton lockButton = new JButton("Bloquear");

    private Note current;
    private String sessionPassword;
    private int placeOffset = 0; // para no apilar elementos nuevos exactamente

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

        // Izquierda: lista.
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

        // Derecha: titulo + pizarra + barra.
        JPanel right = new JPanel(new BorderLayout(0, 8));
        right.setBackground(Theme.BACKGROUND);
        right.setBorder(new EmptyBorder(0, 12, 0, 0));

        titleField.setFont(Theme.fontBold(18));
        titleField.setForeground(Theme.TEXT);
        titleField.putClientProperty("JTextField.placeholderText", "Título de la nota");
        right.add(titleField, BorderLayout.NORTH);

        JScrollPane boardScroll = new JScrollPane(board);
        boardScroll.setBorder(BorderFactory.createLineBorder(Theme.LINE));
        boardScroll.getVerticalScrollBar().setUnitIncrement(16);
        right.add(boardScroll, BorderLayout.CENTER);
        enableFileDrop(board);

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        toolbar.setOpaque(false);
        toolbar.add(plain(new JButton("+ Texto"), e -> addTextBox()));
        toolbar.add(plain(new JButton("+ Imagen"), e -> addImageFromChooser()));
        toolbar.add(plain(new JButton("+ Dibujar"), e -> addDrawing()));
        toolbar.add(plain(new JButton("+ Audio"), e -> addAudioFromChooser()));

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
        board.load(sel.getContent());
        setEditorEnabled(true);
        lockButton.setText(sel.isLocked() ? "Quitar contraseña" : "Bloquear");
    }

    private void saveCurrent() {
        if (current == null) {
            return;
        }
        String t = titleField.getText().trim();
        current.setTitle(t.isEmpty() ? "Sin título" : t);
        current.setContent(board.serialize());
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
        JOptionPane.showMessageDialog(this, "Nota guardada", "Notas", JOptionPane.INFORMATION_MESSAGE);
    }

    // ----- Elementos de la pizarra -----

    private int nextX() {
        placeOffset = (placeOffset + 1) % 8;
        return 40 + placeOffset * 24;
    }

    private int nextY() {
        return 40 + placeOffset * 20;
    }

    private void addTextBox() {
        if (!ensureNote()) {
            return;
        }
        board.addText(nextX(), nextY(), 220, 130, "");
    }

    private void addImageFromChooser() {
        if (!ensureNote()) {
            return;
        }
        javax.swing.JFileChooser fc = new javax.swing.JFileChooser();
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Imágenes", "png", "jpg", "jpeg", "gif"));
        if (fc.showOpenDialog(this) == javax.swing.JFileChooser.APPROVE_OPTION) {
            Path dest = copyToAssets(fc.getSelectedFile());
            if (dest != null) {
                board.addImage(nextX(), nextY(), 280, 220, dest.toString());
            }
        }
    }

    private void addAudioFromChooser() {
        if (!ensureNote()) {
            return;
        }
        javax.swing.JFileChooser fc = new javax.swing.JFileChooser();
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Audio", "wav", "mp3", "m4a", "ogg"));
        if (fc.showOpenDialog(this) == javax.swing.JFileChooser.APPROVE_OPTION) {
            Path dest = copyToAssets(fc.getSelectedFile());
            if (dest != null) {
                board.addAudio(nextX(), nextY(), 220, 70, dest.toString());
            }
        }
    }

    private void addDrawing() {
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
            Path dest = dir.resolve("dibujo-" + System.nanoTime() + ".png");
            ImageIO.write(img, "png", dest.toFile());
            current.getAttachments().add(dest.toString());
            board.addImage(nextX(), nextY(), 300, 230, dest.toString());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "No se pudo guardar el dibujo", "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

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
                    java.awt.Point at = ev.getLocation();
                    for (File f : files) {
                        Path dest = copyToAssets(f);
                        if (dest == null) {
                            continue;
                        }
                        if (isImage(f.getName())) {
                            board.addImage(at.x, at.y, 280, 220, dest.toString());
                        } else {
                            board.addAudio(at.x, at.y, 220, 70, dest.toString());
                        }
                        at = new java.awt.Point(at.x + 20, at.y + 20);
                    }
                } catch (Exception ignore) {
                    // drop invalido
                }
            }
        });
    }

    private Path copyToAssets(File src) {
        if (src == null || !src.isFile() || current == null) {
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
                current.setContent(board.serialize());
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
        board.load("");
    }

    private void setEditorEnabled(boolean enabled) {
        titleField.setEnabled(enabled);
        board.setEnabled(enabled);
        lockButton.setEnabled(enabled);
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
