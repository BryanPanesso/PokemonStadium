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
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Escenario de un Pokémon: plataforma, sprite frontal y animaciones (respiración, embestida al
 * atacar, parpadeo al recibir daño, números flotantes y desmayo en gris).
 *
 * <p>Todas las animaciones se calculan a partir del tiempo transcurrido y se redibujan con un
 * {@link Timer} de Swing, de modo que nunca bloquean el EDT.
 */
class SpriteView extends JComponent {
    private static final int FRAME_MS = 30;
    private static final int MAX_SCALE = 4;

    private final Timer timer = new Timer(FRAME_MS, e -> repaint());
    private final Color accent;
    private final boolean mirrored;
    private final long createdAt = System.currentTimeMillis();
    private final List<FloatingText> floatingTexts = new ArrayList<>();

    private BufferedImage sprite;
    private BufferedImage graySprite;
    private String placeholder = "Elige un Pokémon";
    private boolean loading;
    private boolean fainted;
    private long attackStart = -1;
    private long hitStart = -1;
    private long faintStart = -1;
    private float spriteTop = -1;

    /**
     * @param mirrored voltea el sprite horizontalmente para que los dos Pokémon queden frente a frente
     */
    SpriteView(Color accent, boolean mirrored) {
        this.accent = accent;
        this.mirrored = mirrored;
        setPreferredSize(new Dimension(300, 210));
        setMinimumSize(new Dimension(180, 160));
    }

    @Override
    public void addNotify() {
        super.addNotify();
        timer.start();
    }

    @Override
    public void removeNotify() {
        timer.stop();
        super.removeNotify();
    }

    void setLoading(boolean loading) {
        this.loading = loading;
        repaint();
    }

    /** @param image sprite descargado o {@code null} si no hay imagen disponible */
    void setSprite(BufferedImage image) {
        sprite = image == null ? null : cropTransparentMargins(image);
        graySprite = sprite == null ? null : toGrayscale(sprite);
        placeholder = image == null ? "Sin sprite disponible" : "";
        loading = false;
        fainted = false;
        attackStart = -1;
        hitStart = -1;
        floatingTexts.clear();
        repaint();
    }

    void playAttack() {
        attackStart = System.currentTimeMillis();
    }

    void playHit(String text, Color color) {
        long now = System.currentTimeMillis();
        hitStart = now;
        floatingTexts.add(new FloatingText(text, color, now));
    }

    void setFainted(boolean value) {
        if (value && !fainted) {
            faintStart = System.currentTimeMillis();
        }
        fainted = value;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.smooth(g2);
        long now = System.currentTimeMillis();
        int w = getWidth();
        int h = getHeight();
        float cx = w / 2f;
        float platformY = h - 24;

        float radius = Math.max(40, Math.min(w, h) * 0.62f);
        g2.setPaint(new RadialGradientPaint(new Point2D.Float(cx, platformY - h * 0.38f), radius,
                new float[]{0f, 1f}, new Color[]{Theme.withAlpha(accent, 55), Theme.withAlpha(accent, 0)}));
        g2.fillRect(0, 0, w, h);

        float platformW = Math.min(w * 0.75f, 270);
        float platformH = 36;
        Ellipse2D platform = new Ellipse2D.Float(cx - platformW / 2, platformY - platformH / 2, platformW, platformH);
        g2.setPaint(new GradientPaint(0, platformY - platformH / 2, new Color(0x2C3C62),
                0, platformY + platformH / 2, new Color(0x111A30)));
        g2.fill(platform);
        g2.setColor(Theme.withAlpha(accent, 130));
        g2.setStroke(new BasicStroke(1.5f));
        g2.draw(platform);

        if (loading) {
            paintSpinner(g2, cx, platformY - 52, now);
        } else if (sprite == null) {
            paintPlaceholder(g2, cx, platformY);
        } else {
            paintSprite(g2, cx, platformY, now);
        }
        paintFloatingTexts(g2, cx, now);
        g2.dispose();
    }

    private void paintSprite(Graphics2D g2, float cx, float platformY, long now) {
        double scale = Math.min((getWidth() - 30.0) / sprite.getWidth(), (platformY - 14.0) / sprite.getHeight());
        if (scale >= 2) {
            scale = Math.floor(scale);
        }
        scale = Math.min(scale, MAX_SCALE);
        int dw = (int) Math.round(sprite.getWidth() * scale);
        int dh = (int) Math.round(sprite.getHeight() * scale);

        double dx = 0;
        double dy = fainted ? 0 : Math.sin((now - createdAt) / 380.0) * 3;
        float alpha = 1f;
        BufferedImage image = sprite;

        if (attackStart >= 0) {
            double p = (now - attackStart) / 380.0;
            if (p < 1) {
                dx += Math.sin(p * Math.PI) * 28 * (mirrored ? 1 : -1);
            } else {
                attackStart = -1;
            }
        }
        if (hitStart >= 0) {
            long elapsed = now - hitStart;
            double p = elapsed / 520.0;
            if (p < 1) {
                dx += Math.sin(p * Math.PI * 6) * 8 * (1 - p);
                if ((elapsed / 80) % 2 == 1) {
                    alpha = 0.25f;
                }
            } else {
                hitStart = -1;
            }
        }
        if (fainted) {
            double p = Math.min(1, (now - faintStart) / 700.0);
            dy += p * 14;
            alpha = Math.min(alpha, (float) (1 - 0.55 * p));
            image = graySprite;
        }

        int x = (int) Math.round(cx - dw / 2.0 + dx);
        int y = (int) Math.round(platformY - dh + 6 + dy);
        spriteTop = y;

        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, scale >= 1.5
                ? RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
                : RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setComposite(AlphaComposite.SrcOver.derive(alpha));
        if (mirrored) {
            g2.drawImage(image, x + dw, y, -dw, dh, null);
        } else {
            g2.drawImage(image, x, y, dw, dh, null);
        }
        g2.setComposite(AlphaComposite.SrcOver);
    }

    private void paintSpinner(Graphics2D g2, float cx, float cy, long now) {
        float r = 24;
        g2.setStroke(new BasicStroke(4.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(Theme.withAlpha(Theme.TEXT_MUTED, 60));
        g2.draw(new Ellipse2D.Float(cx - r, cy - r, 2 * r, 2 * r));
        g2.setColor(accent);
        double start = -(now % 1000) * 0.36;
        g2.draw(new Arc2D.Float(cx - r, cy - r, 2 * r, 2 * r, (float) start, 100, Arc2D.OPEN));
        spriteTop = cy - r;
    }

    private void paintPlaceholder(Graphics2D g2, float cx, float platformY) {
        float size = 78;
        float y = platformY - size - 4;
        g2.setComposite(AlphaComposite.SrcOver.derive(0.28f));
        Pokeball.paint(g2, cx - size / 2, y, size, Theme.TEXT_MUTED, Theme.SURFACE, Theme.TEXT_MUTED);
        g2.setComposite(AlphaComposite.SrcOver);
        drawCentered(g2, placeholder, cx, y - 14, Theme.sans(Font.PLAIN, 13f), Theme.TEXT_MUTED);
        spriteTop = y;
    }

    private void paintFloatingTexts(Graphics2D g2, float cx, long now) {
        Font font = Theme.sans(Font.BOLD, 24f);
        float baseY = spriteTop < 0 ? getHeight() * 0.3f : spriteTop + 10;
        Iterator<FloatingText> it = floatingTexts.iterator();
        while (it.hasNext()) {
            FloatingText ft = it.next();
            double p = (now - ft.start) / 1100.0;
            if (p >= 1) {
                it.remove();
                continue;
            }
            int alpha = (int) (p < 0.7 ? 255 : 255 * (1 - p) / 0.3);
            float y = (float) (baseY - p * 38);
            drawCentered(g2, ft.text, cx + 1.5f, y + 1.5f, font, Theme.withAlpha(Color.BLACK, alpha * 3 / 4));
            drawCentered(g2, ft.text, cx, y, font, Theme.withAlpha(ft.color, alpha));
        }
    }

    private static void drawCentered(Graphics2D g2, String text, float cx, float baseline, Font font, Color color) {
        g2.setFont(font);
        FontMetrics fm = g2.getFontMetrics();
        g2.setColor(color);
        g2.drawString(text, cx - fm.stringWidth(text) / 2f, baseline);
    }

    /** Recorta el margen transparente para que el sprite quede apoyado sobre la plataforma. */
    private static BufferedImage cropTransparentMargins(BufferedImage src) {
        int minX = src.getWidth();
        int minY = src.getHeight();
        int maxX = -1;
        int maxY = -1;
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                if ((src.getRGB(x, y) >>> 24) > 16) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        if (maxX < 0) {
            return src;
        }
        BufferedImage out = new BufferedImage(maxX - minX + 1, maxY - minY + 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(src, -minX, -minY, null);
        g.dispose();
        return out;
    }

    private static BufferedImage toGrayscale(BufferedImage src) {
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                int argb = src.getRGB(x, y);
                int a = argb >>> 24;
                int r = (argb >> 16) & 0xFF;
                int gr = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;
                int lum = (int) (0.3 * r + 0.59 * gr + 0.11 * b);
                out.setRGB(x, y, (a << 24) | (lum << 16) | (lum << 8) | lum);
            }
        }
        return out;
    }

    private static final class FloatingText {
        final String text;
        final Color color;
        final long start;

        FloatingText(String text, Color color, long start) {
            this.text = text;
            this.color = color;
            this.start = start;
        }
    }
}
