package stadium.ui;

import javax.swing.JComponent;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Indicador de HP actual: el valor baja con una animación suave y la barra cambia de color
 * (verde &gt; 50%, amarillo &gt; 20%, rojo). La animación usa un {@link Timer} de Swing,
 * así que corre en el EDT sin bloquearlo.
 */
class HpBar extends JComponent {
    private static final int BAR_HEIGHT = 12;

    private final Timer timer;
    private int max;
    private int target;
    private double shown;

    HpBar() {
        timer = new Timer(16, e -> step());
        setPreferredSize(new Dimension(260, 36));
        setFont(Theme.sans(Font.BOLD, 12.5f));
    }

    /** Muestra el valor de inmediato, sin animación. */
    void reset(int maxHp, int hp) {
        timer.stop();
        max = maxHp;
        target = hp;
        shown = hp;
        updateTooltip();
        repaint();
    }

    void clear() {
        reset(0, 0);
    }

    void animateTo(int hp) {
        target = Math.max(0, Math.min(max, hp));
        updateTooltip();
        if (!timer.isRunning()) {
            timer.start();
        }
    }

    private void step() {
        double diff = target - shown;
        double delta = Math.max(0.35, Math.abs(diff) * 0.12);
        if (Math.abs(diff) <= delta) {
            shown = target;
            timer.stop();
        } else {
            shown += Math.signum(diff) * delta;
        }
        repaint();
    }

    private void updateTooltip() {
        setToolTipText(max > 0 ? "HP actual: " + target + " de " + max : null);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.smooth(g2);
        int w = getWidth();
        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();

        g2.setColor(Theme.ACCENT);
        g2.drawString("HP", 0, fm.getAscent());
        String value = max > 0 ? Math.round(shown) + " / " + max : "— / —";
        g2.setColor(Theme.TEXT);
        g2.drawString(value, w - fm.stringWidth(value), fm.getAscent());

        int y = fm.getHeight() + 4;
        g2.setColor(Theme.SURFACE);
        g2.fill(new RoundRectangle2D.Float(0, y, w, BAR_HEIGHT, BAR_HEIGHT, BAR_HEIGHT));
        if (max > 0 && shown > 0) {
            double ratio = shown / max;
            Color color = Theme.hpColor(ratio);
            float fillWidth = (float) Math.max(BAR_HEIGHT, w * ratio);
            g2.setPaint(new GradientPaint(0, y, Theme.mix(color, Color.WHITE, 0.25), 0, y + BAR_HEIGHT, color));
            g2.fill(new RoundRectangle2D.Float(0, y, fillWidth, BAR_HEIGHT, BAR_HEIGHT, BAR_HEIGHT));
        }
        g2.dispose();
    }
}
