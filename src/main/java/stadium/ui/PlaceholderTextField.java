package stadium.ui;

import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.geom.RoundRectangle2D;

/** Campo de texto oscuro y redondeado que muestra una pista mientras está vacío. */
class PlaceholderTextField extends JTextField {
    private final String placeholder;

    PlaceholderTextField(String placeholder, int columns) {
        super(columns);
        this.placeholder = placeholder;
        setOpaque(false);
        setForeground(Theme.TEXT);
        setCaretColor(Theme.ACCENT);
        setSelectionColor(Theme.withAlpha(Theme.ACCENT, 120));
        setSelectedTextColor(Color.WHITE);
        setDisabledTextColor(Theme.DISABLED_TEXT);
        setFont(Theme.sans(Font.PLAIN, 14f));
        setBorder(new EmptyBorder(8, 12, 8, 12));
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                selectAll();
                repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.smooth(g2);
        RoundRectangle2D shape = new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 12, 12);
        g2.setColor(isEnabled() ? Theme.FIELD : Theme.DISABLED);
        g2.fill(shape);
        g2.setStroke(new BasicStroke(isFocusOwner() ? 1.6f : 1f));
        g2.setColor(isFocusOwner() ? Theme.ACCENT : Theme.CARD_BORDER);
        g2.draw(shape);

        if (getText().isEmpty()) {
            Insets in = getInsets();
            g2.setFont(getFont().deriveFont(Font.ITALIC));
            g2.setColor(Theme.TEXT_MUTED);
            int baseline = (getHeight() - g2.getFontMetrics().getHeight()) / 2 + g2.getFontMetrics().getAscent();
            g2.drawString(placeholder, in.left, baseline);
        }
        g2.dispose();
        super.paintComponent(g);
    }
}
