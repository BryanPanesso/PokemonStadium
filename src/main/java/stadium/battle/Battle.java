package stadium.battle;

import stadium.model.Pokemon;

import java.util.Objects;
import java.util.Random;

/**
 * Reglas del combate por turnos. No conoce Swing: todo lo que ocurre se comunica
 * mediante {@link BattleListener}.
 *
 * <p><b>Fórmula de daño</b>:
 * <pre>
 * base = max(1, ATK * rand(0.85..1.15) - DEF * rand(0.35..0.60))
 * daño = max(1, round(base * crítico * efectividad))
 *   crítico     = 1.5 con 10% de probabilidad; si no, 1.0
 *   efectividad = {@link TypeChart#modifier} sobre el primer tipo (1.3 / 0.7 / 1.0)
 * </pre>
 * Es una variante de {@code ATK*random(0-1) - DEF*random(0-1)} con los factores acotados:
 * así el ataque pesa más que la defensa, hay variación entre turnos y el daño nunca es 0,
 * lo que garantiza que el combate siempre termina.
 */
public class Battle {
    public static final long DEFAULT_TURN_DELAY_MS = 900;

    static final double CRITICAL_CHANCE = 0.10;
    static final double CRITICAL_MULTIPLIER = 1.5;
    static final double ATK_FACTOR_MIN = 0.85;
    static final double ATK_FACTOR_RANGE = 0.30;
    static final double DEF_FACTOR_MIN = 0.35;
    static final double DEF_FACTOR_RANGE = 0.25;

    private final Pokemon left;
    private final Pokemon right;
    private final String leftName;
    private final String rightName;
    private final long turnDelayMs;
    private final Random random;

    public Battle(Pokemon left, Pokemon right) {
        this(left, right, DEFAULT_TURN_DELAY_MS);
    }

    /** @param turnDelayMs pausa entre turnos para que el combate se pueda seguir en pantalla */
    public Battle(Pokemon left, Pokemon right, long turnDelayMs) {
        this(left, right, turnDelayMs, new Random());
    }

    Battle(Pokemon left, Pokemon right, long turnDelayMs, Random random) {
        this.left = Objects.requireNonNull(left, "left");
        this.right = Objects.requireNonNull(right, "right");
        if (left == right) {
            throw new IllegalArgumentException("Cada jugador necesita su propia instancia de Pokemon (usa copy()).");
        }
        if (turnDelayMs < 0) {
            throw new IllegalArgumentException("La pausa entre turnos no puede ser negativa.");
        }
        this.turnDelayMs = turnDelayMs;
        this.random = Objects.requireNonNull(random, "random");

        // Si ambos jugadores eligen el mismo Pokémon, los nombres de los eventos deben distinguirse.
        boolean sameName = left.getName().equals(right.getName());
        this.leftName = sameName ? left.getDisplayName() + " (J1)" : left.getDisplayName();
        this.rightName = sameName ? right.getDisplayName() + " (J2)" : right.getDisplayName();
    }

    /** Nombre con el que el Pokémon del jugador 1 aparece en los eventos. */
    public String getLeftName() {
        return leftName;
    }

    /** Nombre con el que el Pokémon del jugador 2 aparece en los eventos. */
    public String getRightName() {
        return rightName;
    }

    /**
     * Ejecuta el combate completo restaurando antes el HP de ambos. Bloquea el hilo que lo
     * llama (hace pausas entre turnos), así que nunca debe invocarse desde el EDT.
     */
    public void fight(BattleListener listener) throws InterruptedException {
        Objects.requireNonNull(listener, "listener");
        left.resetHp();
        right.resetHp();

        boolean speedTie = left.getSpeed() == right.getSpeed();
        Pokemon attacker = chooseFirstAttacker();
        Pokemon defender = opponentOf(attacker);

        listener.onBattleStarted(nameOf(attacker), nameOf(defender), speedTie);
        listener.onHpChanged(leftName, left.getCurrentHp());
        listener.onHpChanged(rightName, right.getCurrentHp());

        while (true) {
            if (turnDelayMs > 0) {
                Thread.sleep(turnDelayMs);
            }

            AttackResult result = attack(attacker, defender);
            listener.onTurn(nameOf(attacker), nameOf(defender), result.damage, result.critical, result.modifier);
            listener.onHpChanged(nameOf(defender), defender.getCurrentHp());

            if (defender.isFainted()) {
                listener.onBattleEnded(nameOf(attacker));
                return;
            }

            Pokemon previousAttacker = attacker;
            attacker = defender;
            defender = previousAttacker;
        }
    }

    /** Mayor Speed ataca primero; en empate se sortea. */
    Pokemon chooseFirstAttacker() {
        if (left.getSpeed() != right.getSpeed()) {
            return left.getSpeed() > right.getSpeed() ? left : right;
        }
        return random.nextBoolean() ? left : right;
    }

    /** Calcula el daño de un ataque y lo aplica al defensor. */
    AttackResult attack(Pokemon attacker, Pokemon defender) {
        double atkFactor = ATK_FACTOR_MIN + random.nextDouble() * ATK_FACTOR_RANGE;
        double defFactor = DEF_FACTOR_MIN + random.nextDouble() * DEF_FACTOR_RANGE;
        double base = Math.max(1, attacker.getAttack() * atkFactor - defender.getDefense() * defFactor);

        boolean critical = random.nextDouble() < CRITICAL_CHANCE;
        double modifier = TypeChart.modifier(attacker.getPrimaryType(), defender.getPrimaryType());
        int damage = (int) Math.max(1, Math.round(base * (critical ? CRITICAL_MULTIPLIER : 1.0) * modifier));

        defender.applyDamage(damage);
        return new AttackResult(damage, critical, modifier);
    }

    private Pokemon opponentOf(Pokemon pokemon) {
        return pokemon == left ? right : left;
    }

    private String nameOf(Pokemon pokemon) {
        return pokemon == left ? leftName : rightName;
    }

    static final class AttackResult {
        final int damage;
        final boolean critical;
        final double modifier;

        AttackResult(int damage, boolean critical, double modifier) {
            this.damage = damage;
            this.critical = critical;
            this.modifier = modifier;
        }
    }
}
