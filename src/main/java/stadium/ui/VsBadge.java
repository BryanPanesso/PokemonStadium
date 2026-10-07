package stadium.ui;

import javax.swing.JComponent;
import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;

/** Emblema circular "VS" con los colores de ambos jugadores. */
class VsBadge extends JComponent {
    private static final int SIZE = 88;

    VsBadge() {
        Dimension d = new Dimension(SIZE, SIZE);
        setPreferredSize(d);
        setMaximumSize(d);
        setMinimumSize(d);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.smooth(g2);
        float x = (getWidth() - SIZE) / 2f + 3;
        float y = 3;
        float d = SIZE - 6;

        g2.setPaint(new GradientPaint(x, y, Theme.PLAYER_1, x + d, y + d, Theme.PLAYER_2));
        g2.fill(new Ellipse2D.Float(x, y, d, d));
        g2.setColor(Theme.SURFACE);
        g2.fill(new Ellipse2D.Float(x + 5, y + 5, d - 10, d - 10));
        g2.setStroke(new BasicStroke(1f));
        g2.setColor(Theme.withAlpha(Theme.ACCENT, 90));
        g2.draw(new Ellipse2D.Float(x + 9, y + 9, d - 18, d - 18));

        g2.setFont(Theme.sans(Font.BOLD | Font.ITALIC, 28f));
        FontMetrics fm = g2.getFontMetrics();
        String text = "VS";
        float tx = x + (d - fm.stringWidth(text)) / 2f;
        float ty = y + (d - fm.getHeight()) / 2f + fm.getAscent();
        g2.setColor(Theme.ACCENT);
        g2.drawString(text, tx, ty);
        g2.dispose();
    }
}
