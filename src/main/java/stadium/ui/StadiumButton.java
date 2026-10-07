package stadium.ui;

import javax.swing.ButtonModel;
import javax.swing.JButton;
import javax.swing.border.EmptyBorder;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;

/** Botón redondeado con estados hover / presionado / deshabilitado / foco dibujados a mano. */
class StadiumButton extends JButton {
    private final Color base;
    private final Color textColor;

    StadiumButton(String text, Color base, Color textColor, float fontSize) {
        super(text);
        this.base = base;
        this.textColor = textColor;
        setFont(Theme.sans(Font.BOLD, fontSize));
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setRolloverEnabled(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(new EmptyBorder(8, 16, 8, 16));
    }

    StadiumButton(String text, Color base) {
        this(text, base, Theme.readableTextOn(base), 13f);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.smooth(g2);
        int w = getWidth();
        int h = getHeight();
        float arc = Math.min(h, 16);
        ButtonModel model = getModel();

        Color fill;
        if (!isEnabled()) {
            fill = Theme.DISABLED;
        } else if (model.isPressed()) {
            fill = Theme.mix(base, Color.BLACK, 0.2);
        } else if (model.isRollover()) {
            fill = Theme.mix(base, Color.WHITE, 0.15);
        } else {
            fill = base;
        }
        g2.setPaint(new GradientPaint(0, 0, Theme.mix(fill, Color.WHITE, 0.10), 0, h, fill));
        g2.fill(new RoundRectangle2D.Float(0, 0, w, h, arc, arc));

        if (isEnabled() && isFocusOwner()) {
            g2.setColor(Theme.withAlpha(Color.WHITE, 170));
            g2.setStroke(new BasicStroke(1.6f));
            g2.draw(new RoundRectangle2D.Float(1.5f, 1.5f, w - 3, h - 3, arc - 2, arc - 2));
        }

        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();
        String text = getText();
        int tx = (w - fm.stringWidth(text)) / 2;
        int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
        g2.setColor(isEnabled() ? textColor : Theme.DISABLED_TEXT);
        g2.drawString(text, tx, ty);
        g2.dispose();
    }
}
