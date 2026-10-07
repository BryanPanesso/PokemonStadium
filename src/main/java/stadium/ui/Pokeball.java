package stadium.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

/** Dibuja una Poké Ball vectorial (logo, ícono de ventana y placeholder del sprite). */
final class Pokeball {
    private Pokeball() {
    }

    static void paint(Graphics2D g, double x, double y, double size, Color top, Color bottom, Color line) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.smooth(g2);
        Ellipse2D circle = new Ellipse2D.Double(x, y, size, size);

        g2.setColor(bottom);
        g2.fill(circle);
        Shape oldClip = g2.getClip();
        g2.clip(new Rectangle2D.Double(x, y, size, size / 2));
        g2.setColor(top);
        g2.fill(circle);
        g2.setClip(oldClip);

        double band = size * 0.08;
        g2.setColor(line);
        g2.fill(new Rectangle2D.Double(x, y + size / 2 - band / 2, size, band));
        g2.setStroke(new BasicStroke((float) Math.max(1, size * 0.06)));
        g2.draw(circle);

        double outer = size * 0.34;
        g2.fill(new Ellipse2D.Double(x + (size - outer) / 2, y + (size - outer) / 2, outer, outer));
        double inner = size * 0.2;
        g2.setColor(bottom);
        g2.fill(new Ellipse2D.Double(x + (size - inner) / 2, y + (size - inner) / 2, inner, inner));
        g2.dispose();
    }

    static BufferedImage image(int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        double pad = size * 0.06;
        paint(g, pad, pad, size - 2 * pad, Theme.PLAYER_1, Color.WHITE, new Color(0x222222));
        g.dispose();
        return img;
    }
}
