package stadium.ui;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.RoundRectangle2D;

/** Barra de desplazamiento minimalista acorde al tema oscuro. */
class DarkScrollBarUI extends BasicScrollBarUI {

    @Override
    protected JButton createDecreaseButton(int orientation) {
        return invisibleButton();
    }

    @Override
    protected JButton createIncreaseButton(int orientation) {
        return invisibleButton();
    }

    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle bounds) {
        g.setColor(Theme.SURFACE);
        g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
    }

    @Override
    protected void paintThumb(Graphics g, JComponent c, Rectangle bounds) {
        if (bounds.isEmpty() || !scrollbar.isEnabled()) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.smooth(g2);
        g2.setColor(isThumbRollover() ? Theme.TEXT_MUTED : Theme.CARD_BORDER);
        g2.fill(new RoundRectangle2D.Float(bounds.x + 3, bounds.y + 2, bounds.width - 6, bounds.height - 4, 8, 8));
        g2.dispose();
    }

    private static JButton invisibleButton() {
        JButton button = new JButton();
        Dimension zero = new Dimension(0, 0);
        button.setPreferredSize(zero);
        button.setMinimumSize(zero);
        button.setMaximumSize(zero);
        return button;
    }
}
