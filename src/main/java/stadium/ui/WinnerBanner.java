package stadium.ui;

import javax.swing.JComponent;
import javax.swing.Timer;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;

/** Cartel "GANADOR" que aparece con un fundido al terminar el combate. */
class WinnerBanner extends JComponent {
    private static final int FADE_MS = 450;

    private final Timer fadeTimer;
    private String winner;
    private Color accent = Theme.ACCENT;
    private long shownAt;

    WinnerBanner() {
        Dimension d = new Dimension(196, 92);
        setPreferredSize(d);
        setMaximumSize(d);
        setMinimumSize(d);
        fadeTimer = new Timer(16, e -> {
            repaint();
            if (System.currentTimeMillis() - shownAt > FADE_MS) {
                ((Timer) e.getSource()).stop();
            }
        });
    }

    void showWinner(String name, Color playerColor) {
        winner = name;
        accent = playerColor;
        shownAt = System.currentTimeMillis();
        setToolTipText("Ganador: " + name);
        fadeTimer.restart();
    }

    void hideBanner() {
        winner = null;
        fadeTimer.stop();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (winner == null) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.smooth(g2);
        float progress = Math.min(1f, (System.currentTimeMillis() - shownAt) / (float) FADE_MS);
        g2.setComposite(AlphaComposite.SrcOver.derive(progress));

        int w = getWidth();
        int h = getHeight();
        float offset = (1 - progress) * 10;
        RoundRectangle2D box = new RoundRectangle2D.Float(2, 2 + offset, w - 4, h - 4, 18, 18);
        g2.setPaint(new GradientPaint(0, 0, Theme.mix(Theme.CARD, accent, 0.35), 0, h, Theme.CARD));
        g2.fill(box);
        g2.setStroke(new BasicStroke(2f));
        g2.setColor(Theme.ACCENT);
        g2.draw(box);

        g2.setFont(Theme.sans(Font.BOLD, 12f));
        FontMetrics small = g2.getFontMetrics();
        String label = "¡GANADOR!";
        g2.setColor(Theme.ACCENT);
        g2.drawString(label, (w - small.stringWidth(label)) / 2f, 26 + offset);

        float size = 22f;
        Font nameFont = Theme.sans(Font.BOLD, size);
        while (size > 12 && g2.getFontMetrics(nameFont).stringWidth(winner) > w - 24) {
            size -= 1;
            nameFont = Theme.sans(Font.BOLD, size);
        }
        g2.setFont(nameFont);
        FontMetrics fm = g2.getFontMetrics();
        g2.setColor(Theme.TEXT);
        g2.drawString(winner, (w - fm.stringWidth(winner)) / 2f, 26 + offset + 14 + fm.getAscent());
        g2.dispose();
    }
}
