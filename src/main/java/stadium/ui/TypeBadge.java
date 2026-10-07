package stadium.ui;

import javax.swing.JLabel;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;

/** Pastilla de color con el nombre del tipo en español (p. ej. FUEGO). */
class TypeBadge extends JLabel {
    private final Color color;

    TypeBadge(String type) {
        super(Theme.upper(Theme.typeName(type)));
        this.color = Theme.typeColor(type);
        setForeground(Theme.readableTextOn(color));
        setFont(Theme.sans(Font.BOLD, 11.5f));
        setBorder(new EmptyBorder(3, 11, 3, 11));
        setToolTipText("Tipo " + Theme.typeName(type) + " (" + type + ")");
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.smooth(g2);
        g2.setColor(color);
        g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), getHeight(), getHeight()));
        g2.dispose();
        super.paintComponent(g);
    }
}
