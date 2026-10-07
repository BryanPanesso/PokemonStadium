package stadium.ui;

import stadium.model.Pokemon;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.Locale;

/**
 * Tarjeta de un jugador: búsqueda (campo + Load + Random), sprite, nombre, tipos,
 * indicador de HP y stats. Solo muestra datos; la carga y el combate los coordina
 * {@link BattleFrame}.
 */
class PokemonCard extends JPanel {
    private static final float ARC = 22f;
    private static final int TYPES_ROW_HEIGHT = 24;

    private final String playerLabel;
    private final Color accent;

    private final PlaceholderTextField nameField = new PlaceholderTextField("Nombre o número (ej. pikachu)", 14);
    private final StadiumButton loadButton;
    private final StadiumButton randomButton = new StadiumButton("Random", Theme.NEUTRAL_BUTTON);
    private final MessageView messageView = new MessageView(2, false, 12.5f);
    private final SpriteView spriteView;
    private final JLabel nameLabel = new JLabel("Sin elegir");
    private final JLabel idLabel = new JLabel("");
    private final JPanel typesPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
    private final HpBar hpBar = new HpBar();
    private final StatBar hpStat = new StatBar("HP", "Puntos de salud base", new Color(0xFF5959));
    private final StatBar attackStat = new StatBar("ATK", "Ataque base", new Color(0xF5AC78));
    private final StatBar defenseStat = new StatBar("DEF", "Defensa base", new Color(0xFAE078));
    private final StatBar speedStat = new StatBar("SPD", "Velocidad base: el más rápido ataca primero", new Color(0xFA92B2));

    private Pokemon pokemon;
    private boolean busy;
    private boolean winner;

    PokemonCard(int playerNumber, Color accent, boolean mirrorSprite) {
        this.playerLabel = "J" + playerNumber;
        this.accent = accent;
        this.loadButton = new StadiumButton("Load", accent);
        this.spriteView = new SpriteView(accent, mirrorSprite);

        setOpaque(false);
        setLayout(new BorderLayout(0, 8));
        setBorder(new EmptyBorder(18, 20, 18, 20));

        String prefix = "p" + playerNumber + ".";
        nameField.setName(prefix + "name");
        loadButton.setName(prefix + "load");
        randomButton.setName(prefix + "random");
        nameField.getAccessibleContext().setAccessibleName("Nombre del Pokémon del jugador " + playerNumber);
        loadButton.setToolTipText("Buscar por nombre (Enter)");
        randomButton.setToolTipText("Elegir un Pokémon al azar");

        add(buildHeader(playerNumber), BorderLayout.NORTH);
        add(spriteView, BorderLayout.CENTER);
        add(buildInfo(), BorderLayout.SOUTH);
        showEmpty();
    }

    private JComponent buildHeader(int playerNumber) {
        JPanel header = transparent(new JPanel());
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("JUGADOR " + playerNumber);
        title.setFont(Theme.sans(Font.BOLD, 13f));
        title.setForeground(accent);

        JPanel searchRow = transparent(new JPanel(new GridBagLayout()));
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        searchRow.add(nameField, c);
        c.weightx = 0;
        c.insets = new Insets(0, 8, 0, 0);
        searchRow.add(loadButton, c);
        searchRow.add(randomButton, c);

        for (JComponent comp : new JComponent[]{title, searchRow, messageView}) {
            comp.setAlignmentX(Component.LEFT_ALIGNMENT);
        }
        header.add(title);
        header.add(Box.createVerticalStrut(8));
        header.add(searchRow);
        header.add(Box.createVerticalStrut(6));
        header.add(messageView);
        return header;
    }

    private JComponent buildInfo() {
        JPanel info = transparent(new JPanel());
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));

        nameLabel.setFont(Theme.sans(Font.BOLD, 26f));
        nameLabel.setForeground(Theme.TEXT_MUTED);
        idLabel.setFont(Theme.sans(Font.BOLD, 15f));
        idLabel.setForeground(Theme.TEXT_MUTED);
        JPanel nameRow = transparent(new JPanel(new BorderLayout()));
        nameRow.add(nameLabel, BorderLayout.CENTER);
        nameRow.add(idLabel, BorderLayout.EAST);

        typesPanel.setOpaque(false);
        typesPanel.setBorder(new EmptyBorder(0, -6, 0, 0));

        JPanel stats = transparent(new JPanel(new GridLayout(4, 1, 0, 2)));
        stats.add(hpStat);
        stats.add(attackStat);
        stats.add(defenseStat);
        stats.add(speedStat);

        JComponent[] rows = {nameRow, typesPanel, hpBar, stats};
        for (JComponent row : rows) {
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
        }
        info.add(nameRow);
        info.add(Box.createVerticalStrut(4));
        info.add(typesPanel);
        info.add(Box.createVerticalStrut(12));
        info.add(hpBar);
        info.add(Box.createVerticalStrut(12));
        info.add(stats);
        return info;
    }

    JTextField getNameField() {
        return nameField;
    }

    JButton getLoadButton() {
        return loadButton;
    }

    JButton getRandomButton() {
        return randomButton;
    }

    SpriteView getSpriteView() {
        return spriteView;
    }

    HpBar getHpBar() {
        return hpBar;
    }

    Pokemon getPokemon() {
        return pokemon;
    }

    String getPlayerLabel() {
        return playerLabel;
    }

    Color getAccent() {
        return accent;
    }

    /** Listo para combatir: hay un Pokémon cargado y no hay una carga en curso. */
    boolean isReady() {
        return pokemon != null && !busy;
    }

    void setInputsEnabled(boolean enabled) {
        nameField.setEnabled(enabled);
        loadButton.setEnabled(enabled);
        randomButton.setEnabled(enabled);
    }

    void showLoading(String message) {
        busy = true;
        setInputsEnabled(false);
        spriteView.setLoading(true);
        messageView.setMessage(message, Theme.TEXT_MUTED);
    }

    void showPokemon(Pokemon loaded, BufferedImage sprite, String warning) {
        busy = false;
        pokemon = loaded;
        winner = false;
        setInputsEnabled(true);
        spriteView.setSprite(sprite);
        nameField.setText(loaded.getName());
        nameLabel.setText(loaded.getDisplayName());
        nameLabel.setForeground(Theme.TEXT);
        idLabel.setText(String.format(Locale.ROOT, "#%04d", loaded.getId()));

        typesPanel.removeAll();
        typesPanel.add(Box.createRigidArea(new Dimension(0, TYPES_ROW_HEIGHT)));
        for (String type : loaded.getTypes()) {
            typesPanel.add(new TypeBadge(type));
        }
        typesPanel.revalidate();
        typesPanel.repaint();

        hpBar.reset(loaded.getMaxHp(), loaded.getMaxHp());
        hpStat.setValue(loaded.getMaxHp());
        attackStat.setValue(loaded.getAttack());
        defenseStat.setValue(loaded.getDefense());
        speedStat.setValue(loaded.getSpeed());

        if (warning != null) {
            messageView.setMessage(warning, Theme.HP_MID);
        } else {
            messageView.setMessage("Listo para combatir.", Theme.SUCCESS);
        }
        repaint();
    }

    /** Muestra el error y conserva el Pokémon cargado anteriormente (si lo hay). */
    void showError(String message) {
        busy = false;
        setInputsEnabled(true);
        spriteView.setLoading(false);
        messageView.setMessage(message, Theme.ERROR);
        nameField.requestFocusInWindow();
    }

    void prepareForBattle() {
        winner = false;
        spriteView.setFainted(false);
        messageView.clear();
        repaint();
    }

    void setWinner(boolean value) {
        winner = value;
        repaint();
    }

    private void showEmpty() {
        typesPanel.add(Box.createRigidArea(new Dimension(0, TYPES_ROW_HEIGHT)));
        hpBar.clear();
        messageView.setMessage("Escribe un nombre y pulsa Load, o prueba Random.", Theme.TEXT_MUTED);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.smooth(g2);
        int w = getWidth();
        int h = getHeight();
        Shape card = new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, ARC, ARC);

        g2.setPaint(new GradientPaint(0, 0, Theme.mix(Theme.CARD, accent, 0.07), 0, h, Theme.CARD));
        g2.fill(card);

        g2.clip(card);
        g2.setColor(accent);
        g2.fillRect(0, 0, w, 4);
        g2.setClip(null);

        g2.setStroke(new BasicStroke(winner ? 2.5f : 1f));
        g2.setColor(winner ? Theme.ACCENT : Theme.CARD_BORDER);
        g2.draw(card);
        g2.dispose();
    }

    private static <T extends JComponent> T transparent(T component) {
        component.setOpaque(false);
        return component;
    }
}
