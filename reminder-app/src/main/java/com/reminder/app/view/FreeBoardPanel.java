package com.reminder.app.view;

import com.reminder.app.config.Theme;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;

/**
 * Pizarra libre: lienzo de posicionamiento absoluto donde se colocan elementos
 * (cajas de texto, imagenes y audio) que el usuario puede **mover y
 * redimensionar** libremente. Cada elemento guarda su posicion y tamaño.
 *
 * Formato de serializacion (una linea por elemento):
 *   TIPO|x|y|w|h|base64(payload)
 * donde payload = texto (TEXT) o ruta de archivo (IMAGE/AUDIO).
 *
 * @author Jesus Gutierrez
 */
public class FreeBoardPanel extends JPanel {

    private static final int HANDLE_H = 22;

    public FreeBoardPanel() {
        setLayout(null); // posicionamiento absoluto = colocacion libre
        setBackground(Theme.BACKGROUND);
        setPreferredSize(new Dimension(1200, 1600));
    }

    public void addText(int x, int y, int w, int h, String text) {
        addItem(new BoardItem("TEXT", text, null), x, y, w, h);
    }

    public void addImage(int x, int y, int w, int h, String path) {
        addItem(new BoardItem("IMAGE", null, path), x, y, w, h);
    }

    public void addAudio(int x, int y, int w, int h, String path) {
        addItem(new BoardItem("AUDIO", null, path), x, y, w, h);
    }

    private void addItem(BoardItem item, int x, int y, int w, int h) {
        item.setBounds(x, y, w, h);
        add(item);
        setComponentZOrder(item, 0);
        revalidate();
        repaint();
    }

    /** Serializa todos los elementos del lienzo. */
    public String serialize() {
        StringBuilder sb = new StringBuilder();
        for (java.awt.Component c : getComponents()) {
            if (c instanceof BoardItem) {
                BoardItem it = (BoardItem) c;
                sb.append(it.getType()).append("|")
                  .append(it.getX()).append("|").append(it.getY()).append("|")
                  .append(it.getWidth()).append("|").append(it.getHeight()).append("|")
                  .append(b64(it.getPayload())).append("\n");
            }
        }
        return sb.toString();
    }

    /** Carga los elementos desde una cadena serializada. */
    public void load(String data) {
        removeAll();
        if (data != null && !data.isBlank()) {
            for (String line : data.split("\n")) {
                if (line.isBlank()) {
                    continue;
                }
                String[] p = line.split("\\|", -1);
                if (p.length < 6) {
                    continue;
                }
                try {
                    int x = Integer.parseInt(p[1]);
                    int y = Integer.parseInt(p[2]);
                    int w = Integer.parseInt(p[3]);
                    int h = Integer.parseInt(p[4]);
                    String payload = unb64(p[5]);
                    switch (p[0]) {
                        case "TEXT":  addText(x, y, w, h, payload); break;
                        case "IMAGE": addImage(x, y, w, h, payload); break;
                        case "AUDIO": addAudio(x, y, w, h, payload); break;
                        default: break;
                    }
                } catch (RuntimeException ignore) {
                    // linea corrupta: se ignora
                }
            }
        }
        revalidate();
        repaint();
    }

    private String b64(String s) {
        return Base64.getEncoder().encodeToString((s == null ? "" : s).getBytes(StandardCharsets.UTF_8));
    }

    private String unb64(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        return new String(Base64.getDecoder().decode(s), StandardCharsets.UTF_8);
    }

    /** Elemento movible y redimensionable del lienzo. */
    private class BoardItem extends JPanel {

        private final String type;
        private final String path;       // para IMAGE / AUDIO
        private JTextArea textArea;       // para TEXT

        BoardItem(String type, String text, String path) {
            this.type = type;
            this.path = path;
            setLayout(new BorderLayout());
            setBackground(Theme.SURFACE);
            setBorder(BorderFactory.createLineBorder(Theme.LINE));

            add(buildHandle(), BorderLayout.NORTH);
            add(buildContent(text), BorderLayout.CENTER);
            add(buildResizeGrip(), BorderLayout.SOUTH);
        }

        private JPanel buildHandle() {
            JPanel handle = new JPanel(new BorderLayout());
            handle.setBackground(Theme.PRIMARY);
            handle.setPreferredSize(new Dimension(10, HANDLE_H));
            handle.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));

            JLabel grip = new JLabel("  ⠿ mover");
            grip.setForeground(Theme.TEXT_ON_PRIMARY);
            grip.setFont(Theme.fontBold(11));
            handle.add(grip, BorderLayout.CENTER);

            JButton del = new JButton("✕");
            del.setForeground(Theme.TEXT_ON_PRIMARY);
            del.setContentAreaFilled(false);
            del.setBorder(new EmptyBorder(0, 6, 0, 6));
            del.setFocusPainted(false);
            del.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            del.addActionListener(e -> {
                java.awt.Container parent = getParent();
                parent.remove(BoardItem.this);
                parent.revalidate();
                parent.repaint();
            });
            handle.add(del, BorderLayout.EAST);

            // Arrastrar el elemento por el handle.
            MouseAdapter drag = new MouseAdapter() {
                private Point origin;

                @Override
                public void mousePressed(MouseEvent e) {
                    origin = e.getPoint();
                    getParent().setComponentZOrder(BoardItem.this, 0);
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    if (origin != null) {
                        int nx = getX() + e.getX() - origin.x;
                        int ny = getY() + e.getY() - origin.y;
                        setLocation(Math.max(0, nx), Math.max(0, ny));
                    }
                }
            };
            handle.addMouseListener(drag);
            handle.addMouseMotionListener(drag);
            grip.addMouseListener(drag);
            grip.addMouseMotionListener(drag);
            return handle;
        }

        private java.awt.Component buildContent(String text) {
            if ("TEXT".equals(type)) {
                textArea = new JTextArea(text == null ? "" : text);
                textArea.setLineWrap(true);
                textArea.setWrapStyleWord(true);
                textArea.setFont(Theme.fontRegular(14));
                textArea.setForeground(Theme.TEXT);
                textArea.setBackground(Color.WHITE);
                textArea.setBorder(new EmptyBorder(6, 6, 6, 6));
                return new JScrollPane(textArea);
            }
            if ("IMAGE".equals(type)) {
                JLabel img = new JLabel();
                img.setHorizontalAlignment(JLabel.CENTER);
                img.setBackground(Color.WHITE);
                img.setOpaque(true);
                File f = new File(path);
                if (f.isFile()) {
                    img.setIcon(scaled(path, 260, 180));
                    img.setToolTipText(f.getName());
                } else {
                    img.setText("(imagen no encontrada)");
                    img.setForeground(Theme.TEXT_MUTED);
                }
                return img;
            }
            // AUDIO
            File f = new File(path);
            JButton play = new JButton("▶  " + (f.getName().length() > 22
                    ? f.getName().substring(0, 21) + "…" : f.getName()));
            play.setFont(Theme.fontBold(13));
            play.setForeground(Theme.PRIMARY_DARK);
            play.setBackground(Color.WHITE);
            play.setFocusPainted(false);
            play.setToolTipText(f.getName());
            play.addActionListener(e -> openFile(f));
            return play;
        }

        private JPanel buildResizeGrip() {
            JPanel bottom = new JPanel(new BorderLayout());
            bottom.setOpaque(false);
            JLabel grip = new JLabel("◢");
            grip.setForeground(Theme.TEXT_MUTED);
            grip.setHorizontalAlignment(JLabel.RIGHT);
            grip.setCursor(Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR));
            grip.setBorder(new EmptyBorder(0, 0, 2, 4));

            MouseAdapter resize = new MouseAdapter() {
                private int sx, sy, sw, sh;

                @Override
                public void mousePressed(MouseEvent e) {
                    sx = e.getXOnScreen();
                    sy = e.getYOnScreen();
                    sw = getWidth();
                    sh = getHeight();
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    int nw = Math.max(120, sw + e.getXOnScreen() - sx);
                    int nh = Math.max(70, sh + e.getYOnScreen() - sy);
                    setSize(nw, nh);
                    revalidate();
                    repaint();
                }
            };
            grip.addMouseListener(resize);
            grip.addMouseMotionListener(resize);
            bottom.add(grip, BorderLayout.EAST);
            return bottom;
        }

        private ImageIcon scaled(String p, int w, int h) {
            Image img = new ImageIcon(p).getImage();
            int iw = img.getWidth(null);
            int ih = img.getHeight(null);
            if (iw <= 0 || ih <= 0) {
                return new ImageIcon(p);
            }
            double r = Math.min((double) w / iw, (double) h / ih);
            return new ImageIcon(img.getScaledInstance(
                    Math.max(1, (int) (iw * r)), Math.max(1, (int) (ih * r)), Image.SCALE_SMOOTH));
        }

        private void openFile(File f) {
            try {
                if (f.isFile() && java.awt.Desktop.isDesktopSupported()) {
                    java.awt.Desktop.getDesktop().open(f);
                }
            } catch (Exception ignore) {
                // no se pudo abrir
            }
        }

        String getType() {
            return type;
        }

        String getPayload() {
            return "TEXT".equals(type) ? textArea.getText() : path;
        }
    }
}
