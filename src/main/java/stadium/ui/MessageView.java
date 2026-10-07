package stadium.ui;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Texto de estado con salto de línea automático y alto fijo (no hace "saltar" el layout
 * cuando aparece o desaparece un mensaje).
 */
class MessageView extends JComponent {
    private final int maxLines;
    private final boolean centered;
    private String text = "";
    private Color color = Theme.TEXT_MUTED;

    MessageView(int maxLines, boolean centered, float fontSize) {
        this.maxLines = maxLines;
        this.centered = centered;
        setFont(Theme.sans(Font.PLAIN, fontSize));
        FontMetrics fm = getFontMetrics(getFont());
        Dimension size = new Dimension(60, fm.getHeight() * maxLines + 2);
        setPreferredSize(size);
        setMinimumSize(size);
    }

    void setMessage(String text, Color color) {
        this.text = text == null ? "" : text;
        this.color = color;
        setToolTipText(this.text.isEmpty() ? null : this.text);
        repaint();
    }

    void clear() {
        setMessage("", Theme.TEXT_MUTED);
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (text.isEmpty()) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.smooth(g2);
        g2.setFont(getFont());
        g2.setColor(color);
        FontMetrics fm = g2.getFontMetrics();
        List<String> lines = wrap(text, fm, getWidth());
        int y = fm.getAscent();
        for (String line : lines) {
            int x = centered ? (getWidth() - fm.stringWidth(line)) / 2 : 0;
            g2.drawString(line, x, y);
            y += fm.getHeight();
        }
        g2.dispose();
    }

    private List<String> wrap(String value, FontMetrics fm, int width) {
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : value.split(" ")) {
            String candidate = current.length() == 0 ? word : current + " " + word;
            if (fm.stringWidth(candidate) <= width || current.length() == 0) {
                current.setLength(0);
                current.append(candidate);
            } else {
                lines.add(current.toString());
                current.setLength(0);
                current.append(word);
            }
        }
        lines.add(current.toString());

        if (lines.size() > maxLines) {
            List<String> visible = new ArrayList<>(lines.subList(0, maxLines));
            String last = visible.get(maxLines - 1);
            while (!last.isEmpty() && fm.stringWidth(last + "…") > width) {
                last = last.substring(0, last.length() - 1);
            }
            visible.set(maxLines - 1, last + "…");
            return visible;
        }
        return lines;
    }
}
