package com.reminder.app.view;

import com.reminder.app.config.Theme;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

/**
 * Lienzo de dibujo a mano alzada. Devuelve la imagen dibujada (o null si se
 * cancela) para insertarla en la nota.
 *
 * @author Jesus Gutierrez
 */
public class SketchDialog extends JDialog {

    private final BufferedImage canvas;
    private final Graphics2D g2;
    private Point last;
    private boolean accepted;

    public SketchDialog(java.awt.Window owner) {
        super(owner, "Dibujar", ModalityType.APPLICATION_MODAL);
        int w = 520;
        int h = 360;
        canvas = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        g2 = canvas.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, w, h);
        g2.setColor(Theme.TEXT);
        g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        JPanel board = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(canvas, 0, 0, null);
            }
        };
        board.setPreferredSize(new Dimension(w, h));
        board.setBackground(Color.WHITE);
        board.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                last = e.getPoint();
            }
        });
        board.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (last != null) {
                    g2.drawLine(last.x, last.y, e.getX(), e.getY());
                    last = e.getPoint();
                    board.repaint();
                }
            }
        });

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
        buttons.add(button("Limpiar", e -> {
            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, w, h);
            g2.setColor(Theme.TEXT);
            board.repaint();
        }));
        buttons.add(button("Cancelar", e -> {
            accepted = false;
            dispose();
        }));
        JButton ok = button("Insertar", e -> {
            accepted = true;
            dispose();
        });
        ok.setBackground(Theme.PRIMARY);
        ok.setForeground(Theme.TEXT_ON_PRIMARY);
        buttons.add(ok);

        JPanel content = new JPanel(new BorderLayout(0, 6));
        content.setBorder(new EmptyBorder(10, 10, 10, 10));
        content.add(board, BorderLayout.CENTER);
        content.add(buttons, BorderLayout.SOUTH);
        setContentPane(content);
        pack();
        setLocationRelativeTo(owner);
    }

    private JButton button(String text, java.awt.event.ActionListener a) {
        JButton b = new JButton(text);
        b.setFont(Theme.fontBold(13));
        b.setFocusPainted(false);
        b.addActionListener(a);
        return b;
    }

    /** Muestra el dialogo y devuelve la imagen dibujada, o null si se cancelo. */
    public BufferedImage showAndGet() {
        setVisible(true);
        return accepted ? canvas : null;
    }
}
