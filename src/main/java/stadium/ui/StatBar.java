package stadium.ui;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;

/** Fila "ATK  52  ▬▬▬▬" con el valor numérico y una barra proporcional al stat base. */
class StatBar extends JComponent {
    /** Los stats base rara vez superan 180; por encima la barra se muestra llena. */
    private static final double SCALE_MAX = 180.0;
    private static final int LABEL_WIDTH = 40;
    private static final int VALUE_WIDTH = 34;

    private final String label;
    private final Color color;
    private int value = -1;

    StatBar(String label, String description, Color color) {
        this.label = label;
        this.color = color;
        setToolTipText(description);
        setPreferredSize(new Dimension(260, 20));
        setFont(Theme.sans(Font.BOLD, 12f));
    }

    void setValue(int value) {
        this.value = value;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.smooth(g2);
        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();
        int baseline = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();

        g2.setColor(Theme.TEXT_MUTED);
        g2.drawString(label, 0, baseline);
        String text = value < 0 ? "—" : String.valueOf(value);
        g2.setColor(Theme.TEXT);
        g2.drawString(text, LABEL_WIDTH + VALUE_WIDTH - 8 - fm.stringWidth(text), baseline);

        int x = LABEL_WIDTH + VALUE_WIDTH;
        int barWidth = getWidth() - x;
        int barHeight = 6;
        int y = (getHeight() - barHeight) / 2;
        g2.setColor(Theme.SURFACE);
        g2.fill(new RoundRectangle2D.Float(x, y, barWidth, barHeight, barHeight, barHeight));
        if (value > 0) {
            float width = (float) Math.max(barHeight, barWidth * Math.min(1.0, value / SCALE_MAX));
            g2.setColor(color);
            g2.fill(new RoundRectangle2D.Float(x, y, width, barHeight, barHeight, barHeight));
        }
        g2.dispose();
    }
}
