package stadium.ui;

import stadium.api.PokeApiClient;
import stadium.api.PokeApiException;
import stadium.battle.Battle;
import stadium.battle.BattleListener;
import stadium.model.Pokemon;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.ColorUIResource;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Ventana principal (la parte de UI de "Battle"). Coordina la carga de Pokémon en segundo plano,
 * el inicio del combate y la presentación de sus eventos.
 *
 * <p>Reglas de hilos: las peticiones HTTP y el combate corren en {@link SwingWorker}; todo lo que
 * toca componentes Swing ocurre en el EDT. Durante el combate la UI solo cambia a partir de los
 * eventos de {@link BattleListener}.
 */
public class BattleFrame extends JFrame {

    /** Velocidad del combate: pausa entre turnos. */
    private enum BattleSpeed {
        NORMAL("Normal", Battle.DEFAULT_TURN_DELAY_MS),
        FAST("Rápida", 400),
        SLOW("Lenta", 1500);

        final String label;
        final long delayMs;

        BattleSpeed(String label, long delayMs) {
            this.label = label;
            this.delayMs = delayMs;
        }

        BattleSpeed next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    /** Operación de carga que se ejecuta fuera del EDT. */
    private interface PokemonFetch {
        Pokemon fetch() throws PokeApiException;
    }

    private final PokeApiClient api;
    private final PokemonCard[] cards = {
            new PokemonCard(1, Theme.PLAYER_1, true),
            new PokemonCard(2, Theme.PLAYER_2, false)
    };
    private final StadiumButton fightButton = new StadiumButton("Fight!", Theme.ACCENT, new Color(0x1A1A1A), 22f);
    private final StadiumButton speedButton = new StadiumButton("", Theme.NEUTRAL_BUTTON);
    private final StadiumButton clearLogButton = new StadiumButton("Limpiar", Theme.NEUTRAL_BUTTON, Theme.TEXT, 12f);
    private final MessageView fightHint = new MessageView(3, true, 12.5f);
    private final WinnerBanner winnerBanner = new WinnerBanner();
    private final JTextArea battleLog = new JTextArea(8, 60);
    private final JLabel statusLabel = new JLabel();

    private BattleSpeed speed = BattleSpeed.NORMAL;
    private boolean battleRunning;
    private int battleCount;
    private int turnNumber;
    private Map<String, PokemonCard> cardsByBattleName = new HashMap<>();

    public BattleFrame(PokeApiClient api) {
        super("Pokémon Stadium Lite");
        this.api = api;
        installLookAndFeelDefaults();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setIconImages(java.util.List.of(Pokeball.image(16), Pokeball.image(32), Pokeball.image(64)));

        JPanel root = new BackgroundPanel();
        root.setLayout(new BorderLayout(0, 14));
        root.setBorder(new EmptyBorder(14, 20, 12, 20));
        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(buildArena(), BorderLayout.CENTER);
        root.add(buildBottom(), BorderLayout.SOUTH);
        setContentPane(root);

        registerListeners();
        updateSpeedButton();
        updateFightState();
        setStatus("Listo. Carga un Pokémon para cada jugador.", Theme.TEXT_MUTED);

        fitToScreen(new Dimension(1240, 900), new Dimension(1040, 780));
        setLocationRelativeTo(null);
    }

    // ------------------------------------------------------------------ construcción de la UI

    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout(14, 0));
        header.setOpaque(false);

        JComponent logo = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Pokeball.paint((Graphics2D) g, 2, 2, 40, Theme.PLAYER_1, Color.WHITE, new Color(0x1A1A1A));
            }
        };
        logo.setPreferredSize(new Dimension(44, 44));

        JLabel title = new JLabel("POKÉMON STADIUM LITE");
        title.setFont(Theme.sans(Font.BOLD, 24f));
        title.setForeground(Theme.ACCENT);
        JLabel subtitle = new JLabel("Combate por turnos con datos en vivo de PokeAPI");
        subtitle.setFont(Theme.sans(Font.PLAIN, 13f));
        subtitle.setForeground(Theme.TEXT_MUTED);

        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        titles.add(title);
        titles.add(subtitle);

        JLabel hint = new JLabel("Atajos: Enter = Load · Ctrl+Enter = Fight! · Ctrl+L = limpiar log");
        hint.setFont(Theme.sans(Font.PLAIN, 12f));
        hint.setForeground(Theme.TEXT_MUTED);

        header.add(logo, BorderLayout.WEST);
        header.add(titles, BorderLayout.CENTER);
        header.add(hint, BorderLayout.EAST);
        return header;
    }

    private JComponent buildArena() {
        JPanel arena = new JPanel(new GridBagLayout());
        arena.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;

        c.gridx = 0;
        c.weightx = 0.5;
        arena.add(cards[0], c);

        c.gridx = 1;
        c.weightx = 0;
        c.insets = new Insets(0, 14, 0, 14);
        arena.add(buildCenterColumn(), c);

        c.gridx = 2;
        c.weightx = 0.5;
        c.insets = new Insets(0, 0, 0, 0);
        arena.add(cards[1], c);
        return arena;
    }

    private JComponent buildCenterColumn() {
        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.setPreferredSize(new Dimension(200, 10));

        fightButton.setName("fight");
        fightButton.setMnemonic(KeyEvent.VK_F);
        fightButton.setToolTipText("Iniciar el combate (Ctrl+Enter)");
        Dimension fightSize = new Dimension(184, 58);
        fightButton.setPreferredSize(fightSize);
        fightButton.setMaximumSize(fightSize);

        Dimension speedSize = new Dimension(184, 36);
        speedButton.setPreferredSize(speedSize);
        speedButton.setMaximumSize(speedSize);
        speedButton.setToolTipText("Cambiar la pausa entre turnos");

        fightHint.setMaximumSize(new Dimension(196, fightHint.getPreferredSize().height));

        VsBadge vs = new VsBadge();
        for (JComponent comp : new JComponent[]{vs, fightButton, fightHint, speedButton, winnerBanner}) {
            comp.setAlignmentX(Component.CENTER_ALIGNMENT);
        }
        column.add(Box.createVerticalGlue());
        column.add(vs);
        column.add(Box.createVerticalStrut(22));
        column.add(fightButton);
        column.add(Box.createVerticalStrut(10));
        column.add(fightHint);
        column.add(Box.createVerticalStrut(10));
        column.add(speedButton);
        column.add(Box.createVerticalStrut(22));
        column.add(winnerBanner);
        column.add(Box.createVerticalGlue());
        return column;
    }

    private JComponent buildBottom() {
        JPanel bottom = new JPanel(new BorderLayout(0, 8));
        bottom.setOpaque(false);

        JLabel title = new JLabel("REGISTRO DE BATALLA");
        title.setFont(Theme.sans(Font.BOLD, 13f));
        title.setForeground(Theme.TEXT);
        clearLogButton.setBorder(new EmptyBorder(4, 12, 4, 12));
        clearLogButton.setToolTipText("Vaciar el registro (Ctrl+L)");
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.add(title, BorderLayout.WEST);
        titleRow.add(clearLogButton, BorderLayout.EAST);

        battleLog.setName("log");
        battleLog.setEditable(false);
        battleLog.setLineWrap(true);
        battleLog.setWrapStyleWord(true);
        battleLog.setFont(Theme.mono(13f));
        battleLog.setBackground(Theme.SURFACE);
        battleLog.setForeground(Theme.TEXT);
        battleLog.setCaretColor(Theme.ACCENT);
        battleLog.setSelectionColor(Theme.withAlpha(Theme.ACCENT, 110));
        battleLog.setMargin(new Insets(10, 12, 10, 12));
        battleLog.getAccessibleContext().setAccessibleName("Registro de batalla");

        JScrollPane scroll = new JScrollPane(battleLog);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.CARD_BORDER));
        scroll.getViewport().setBackground(Theme.SURFACE);
        scroll.getVerticalScrollBar().setUI(new DarkScrollBarUI());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setPreferredSize(new Dimension(100, 190));

        statusLabel.setFont(Theme.sans(Font.PLAIN, 12.5f));
        JLabel source = new JLabel("Datos: pokeapi.co/api/v2/pokemon");
        source.setFont(Theme.sans(Font.PLAIN, 12f));
        source.setForeground(Theme.TEXT_MUTED);
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setOpaque(false);
        statusBar.add(statusLabel, BorderLayout.WEST);
        statusBar.add(source, BorderLayout.EAST);

        bottom.add(titleRow, BorderLayout.NORTH);
        bottom.add(scroll, BorderLayout.CENTER);
        bottom.add(statusBar, BorderLayout.SOUTH);
        return bottom;
    }

    // ------------------------------------------------------------------ listeners de botones

    private void registerListeners() {
        for (PokemonCard card : cards) {
            ActionListener loadListener = e -> loadByName(card);
            card.getLoadButton().addActionListener(loadListener);
            card.getNameField().addActionListener(loadListener);
            card.getRandomButton().addActionListener(e -> loadRandom(card));
        }
        fightButton.addActionListener(e -> startBattle());
        speedButton.addActionListener(e -> {
            speed = speed.next();
            updateSpeedButton();
        });
        clearLogButton.addActionListener(e -> battleLog.setText(""));

        JComponent root = getRootPane();
        bindKey(root, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK), "fight", fightButton::doClick);
        bindKey(root, KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_DOWN_MASK), "clearLog", clearLogButton::doClick);
    }

    private static void bindKey(JComponent root, KeyStroke key, String name, Runnable action) {
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(key, name);
        root.getActionMap().put(name, new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                action.run();
            }
        });
    }

    // ------------------------------------------------------------------ carga de Pokémon

    private void loadByName(PokemonCard card) {
        if (!card.getLoadButton().isEnabled()) {
            return;
        }
        String input = card.getNameField().getText();
        if (input.isBlank()) {
            card.showError("Escribe el nombre o número de un Pokémon.");
            return;
        }
        loadAsync(card, "Buscando \"" + input.trim() + "\"…", () -> api.fetchByName(input));
    }

    private void loadRandom(PokemonCard card) {
        loadAsync(card, "Buscando un Pokémon aleatorio…", api::fetchRandom);
    }

    /** Descarga datos y sprite en un SwingWorker; el resultado se aplica en el EDT (done). */
    private void loadAsync(PokemonCard card, String message, PokemonFetch fetch) {
        card.showLoading(message);
        winnerBanner.hideBanner();
        for (PokemonCard c : cards) {
            c.setWinner(false);
        }
        setStatus(card.getPlayerLabel() + ": " + message, Theme.TEXT_MUTED);
        updateFightState();

        new SwingWorker<LoadResult, Void>() {
            @Override
            protected LoadResult doInBackground() throws PokeApiException {
                Pokemon pokemon = fetch.fetch();
                BufferedImage sprite = null;
                String warning = null;
                try {
                    sprite = api.fetchSprite(pokemon.getSpriteUrl());
                    if (sprite == null) {
                        warning = "Listo para combatir (este Pokémon no tiene sprite en PokeAPI).";
                    }
                } catch (PokeApiException e) {
                    warning = "Listo para combatir, pero el sprite no cargó: " + e.getMessage();
                }
                return new LoadResult(pokemon, sprite, warning);
            }

            @Override
            protected void done() {
                try {
                    LoadResult result = get();
                    Pokemon p = result.pokemon;
                    card.showPokemon(p, result.sprite, result.warning);
                    log(String.format(Locale.ROOT, "%s eligió a %s #%04d (%s) · HP %d · ATK %d · DEF %d · SPD %d",
                            card.getPlayerLabel(), p.getDisplayName(), p.getId(), typesText(p),
                            p.getMaxHp(), p.getAttack(), p.getDefense(), p.getSpeed()));
                    setStatus(card.getPlayerLabel() + ": " + p.getDisplayName() + " cargado.", Theme.SUCCESS);
                } catch (ExecutionException ex) {
                    String error = userMessage(ex.getCause());
                    card.showError(error);
                    log("[!] " + card.getPlayerLabel() + ": " + error);
                    setStatus(card.getPlayerLabel() + ": " + error, Theme.ERROR);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    card.showError("La carga fue interrumpida.");
                } finally {
                    updateFightState();
                }
            }
        }.execute();
    }

    // ------------------------------------------------------------------ combate

    private void startBattle() {
        if (!canFight()) {
            return;
        }
        Battle battle = new Battle(cards[0].getPokemon(), cards[1].getPokemon(), speed.delayMs);
        cardsByBattleName = new HashMap<>();
        cardsByBattleName.put(battle.getLeftName(), cards[0]);
        cardsByBattleName.put(battle.getRightName(), cards[1]);

        battleRunning = true;
        battleCount++;
        turnNumber = 0;
        winnerBanner.hideBanner();
        for (PokemonCard card : cards) {
            card.setInputsEnabled(false);
            card.prepareForBattle();
        }
        updateFightState();
        setStatus("Combate en curso…", Theme.ACCENT);

        BattleListener listener = new EdtBattleListener(new UiBattleListener());
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws InterruptedException {
                battle.fight(listener);
                return null;
            }

            @Override
            protected void done() {
                battleRunning = false;
                for (PokemonCard card : cards) {
                    card.setInputsEnabled(true);
                }
                updateFightState();
                try {
                    get();
                } catch (ExecutionException ex) {
                    String error = "El combate se detuvo por un error: " + userMessage(ex.getCause());
                    log("[!] " + error);
                    setStatus(error, Theme.ERROR);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    private boolean canFight() {
        return !battleRunning && cards[0].isReady() && cards[1].isReady();
    }

    /** "Fight!" solo se habilita cuando ambos Pokémon están cargados y no hay nada en curso. */
    private void updateFightState() {
        boolean ready = canFight();
        fightButton.setEnabled(ready);
        speedButton.setEnabled(!battleRunning);
        if (battleRunning) {
            fightHint.setMessage("Combate en curso…", Theme.ACCENT);
        } else if (ready) {
            fightHint.setMessage(battleCount == 0 ? "¡Todo listo! Pulsa Fight!" : "Pulsa Fight! para la revancha.", Theme.SUCCESS);
        } else {
            fightHint.setMessage("Carga un Pokémon para cada jugador para habilitar Fight!", Theme.TEXT_MUTED);
        }
    }

    private void updateSpeedButton() {
        speedButton.setText("Velocidad: " + speed.label);
    }

    /**
     * Traduce los eventos del combate a cambios visuales. Siempre se ejecuta en el EDT
     * gracias a {@link EdtBattleListener}.
     */
    private final class UiBattleListener implements BattleListener {

        @Override
        public void onBattleStarted(String firstAttacker, String secondAttacker, boolean speedTie) {
            log("");
            log("══════ Combate #" + battleCount + ": " + cards[0].getPokemon().getDisplayName()
                    + " vs " + cards[1].getPokemon().getDisplayName() + " ══════");
            int firstSpeed = card(firstAttacker).getPokemon().getSpeed();
            int secondSpeed = card(secondAttacker).getPokemon().getSpeed();
            if (speedTie) {
                log("Empate en Speed (" + firstSpeed + "). Sorteo: " + firstAttacker + " ataca primero.");
            } else {
                log(firstAttacker + " es más rápido (Speed " + firstSpeed + " vs " + secondSpeed + ") y ataca primero.");
            }
        }

        @Override
        public void onTurn(String attacker, String defender, int damage, boolean critical, double modifier) {
            turnNumber++;
            card(attacker).getSpriteView().playAttack();

            Color floatColor = Theme.TEXT;
            if (critical) {
                floatColor = Theme.ACCENT;
            } else if (modifier > 1.0) {
                floatColor = new Color(0xFF9F43);
            } else if (modifier < 1.0) {
                floatColor = Theme.TEXT_MUTED;
            }
            card(defender).getSpriteView().playHit((critical ? "¡Crítico! -" : "-") + damage, floatColor);

            StringBuilder line = new StringBuilder();
            line.append(String.format(Locale.ROOT, "[Turno %d] %s ataca a %s: %d de daño", turnNumber, attacker, defender, damage));
            if (critical) {
                line.append(" · ¡Golpe crítico! (x1.5)");
            }
            if (modifier > 1.0) {
                line.append(String.format(Locale.ROOT, " · ¡Es muy eficaz! (x%.1f)", modifier));
            } else if (modifier < 1.0) {
                line.append(String.format(Locale.ROOT, " · No es muy eficaz… (x%.1f)", modifier));
            }
            log(line.toString());
        }

        @Override
        public void onHpChanged(String pokemon, int hpActual) {
            PokemonCard card = card(pokemon);
            card.getHpBar().animateTo(hpActual);
            if (turnNumber > 0) {
                log("          " + pokemon + ": HP restante " + hpActual + "/" + card.getPokemon().getMaxHp());
            }
            if (hpActual == 0) {
                card.getSpriteView().setFainted(true);
                log(pokemon + " se debilitó.");
            }
        }

        @Override
        public void onBattleEnded(String winner) {
            PokemonCard card = card(winner);
            card.setWinner(true);
            winnerBanner.showWinner(winner, card.getAccent());
            String fullName = withPlayer(winner, card);
            log("» ¡" + fullName + " gana el combate en " + turnNumber + (turnNumber == 1 ? " turno!" : " turnos!"));
            setStatus("Ganador: " + fullName, Theme.SUCCESS);
        }

        /** "Squirtle (J1)"; si el nombre ya trae el jugador (mismo Pokémon en ambos lados) no se repite. */
        private String withPlayer(String battleName, PokemonCard card) {
            String suffix = "(" + card.getPlayerLabel() + ")";
            return battleName.endsWith(suffix) ? battleName : battleName + " " + suffix;
        }

        private PokemonCard card(String battleName) {
            PokemonCard card = cardsByBattleName.get(battleName);
            if (card == null) {
                throw new IllegalStateException("Evento de un Pokémon desconocido: " + battleName);
            }
            return card;
        }
    }

    // ------------------------------------------------------------------ utilidades

    private void log(String line) {
        battleLog.append(line + "\n");
        battleLog.setCaretPosition(battleLog.getDocument().getLength());
    }

    private void setStatus(String text, Color color) {
        statusLabel.setText(text);
        statusLabel.setForeground(color);
    }

    private static String typesText(Pokemon p) {
        StringBuilder sb = new StringBuilder();
        for (String type : p.getTypes()) {
            if (sb.length() > 0) {
                sb.append("/");
            }
            sb.append(Theme.typeName(type));
        }
        return sb.toString();
    }

    private static String userMessage(Throwable error) {
        if (error instanceof PokeApiException) {
            return error.getMessage();
        }
        return "Error inesperado (" + error.getClass().getSimpleName() + ").";
    }

    private void fitToScreen(Dimension preferred, Dimension minimum) {
        Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        setMinimumSize(new Dimension(Math.min(minimum.width, screen.width), Math.min(minimum.height, screen.height)));
        setSize(Math.min(preferred.width, screen.width), Math.min(preferred.height, screen.height));
    }

    private static void installLookAndFeelDefaults() {
        UIManager.put("ToolTip.background", new ColorUIResource(Theme.SURFACE));
        UIManager.put("ToolTip.foreground", new ColorUIResource(Theme.TEXT));
        UIManager.put("ToolTip.border", BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.CARD_BORDER), new EmptyBorder(4, 8, 4, 8)));
        UIManager.put("ToolTip.font", Theme.sans(Font.PLAIN, 12f));
    }

    private static final class LoadResult {
        final Pokemon pokemon;
        final BufferedImage sprite;
        final String warning;

        LoadResult(Pokemon pokemon, BufferedImage sprite, String warning) {
            this.pokemon = pokemon;
            this.sprite = sprite;
            this.warning = warning;
        }
    }

    /** Fondo con degradado y dos halos con los colores de los jugadores. */
    private static final class BackgroundPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            int w = getWidth();
            int h = getHeight();
            g2.setPaint(new GradientPaint(0, 0, Theme.BG_TOP, 0, h, Theme.BG_BOTTOM));
            g2.fillRect(0, 0, w, h);
            paintGlow(g2, w * 0.18f, h * 0.35f, w * 0.35f, Theme.PLAYER_1);
            paintGlow(g2, w * 0.82f, h * 0.35f, w * 0.35f, Theme.PLAYER_2);
            g2.dispose();
        }

        private static void paintGlow(Graphics2D g2, float x, float y, float radius, Color color) {
            g2.setPaint(new RadialGradientPaint(new Point2D.Float(x, y), Math.max(1, radius),
                    new float[]{0f, 1f}, new Color[]{Theme.withAlpha(color, 38), Theme.withAlpha(color, 0)}));
            g2.fillRect(0, 0, (int) (x + radius) + 1, (int) (y + radius) + 1);
        }
    }
}
