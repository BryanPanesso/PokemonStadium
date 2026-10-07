package stadium.battle;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import stadium.model.Pokemon;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BattleTest {

    private static Pokemon pokemon(String name, String type, int hp, int atk, int def, int spd) {
        return new Pokemon(1, name, "", List.of(type), hp, atk, def, spd);
    }

    /** Registra los eventos en orden para poder verificarlos. */
    private static final class RecordingListener implements BattleListener {
        final List<String> events = new ArrayList<>();
        String first;
        String winner;
        int turns;

        @Override
        public void onBattleStarted(String firstAttacker, String secondAttacker, boolean speedTie) {
            first = firstAttacker;
            events.add("start:" + firstAttacker);
        }

        @Override
        public void onTurn(String attacker, String defender, int damage, boolean critical, double modifier) {
            turns++;
            events.add("turn:" + attacker + ">" + defender + ":" + damage);
            assertTrue(damage >= 1, "el daño mínimo es 1");
            assertTrue(modifier == 1.0 || modifier == 1.3 || modifier == 0.7);
        }

        @Override
        public void onHpChanged(String pokemon, int hpActual) {
            events.add("hp:" + pokemon + ":" + hpActual);
            assertTrue(hpActual >= 0, "el HP nunca es negativo");
        }

        @Override
        public void onBattleEnded(String winner) {
            this.winner = winner;
            events.add("end:" + winner);
        }
    }

    @Test
    void elMasRapidoAtacaPrimero() throws InterruptedException {
        RecordingListener listener = new RecordingListener();
        new Battle(pokemon("lento", "normal", 50, 50, 50, 30),
                pokemon("rapido", "normal", 50, 50, 50, 90), 0, new Random(1)).fight(listener);

        assertEquals("Rapido", listener.first);
        assertTrue(listener.events.get(3).startsWith("turn:Rapido>"));
    }

    @Test
    void empateDeSpeedSeSorteaYAmbosPuedenEmpezar() {
        boolean leftStarted = false;
        boolean rightStarted = false;
        Pokemon a = pokemon("a", "normal", 50, 50, 50, 70);
        Pokemon b = pokemon("b", "normal", 50, 50, 50, 70);
        Battle battle = new Battle(a, b, 0, new Random(42));
        for (int i = 0; i < 50; i++) {
            Pokemon first = battle.chooseFirstAttacker();
            leftStarted |= first == a;
            rightStarted |= first == b;
        }
        assertTrue(leftStarted && rightStarted);
    }

    @RepeatedTest(20)
    void elCombateTerminaConUnGanadorYElPerdedorEnCero() throws InterruptedException {
        Pokemon a = pokemon("charmander", "fire", 39, 52, 43, 65);
        Pokemon b = pokemon("bulbasaur", "grass", 45, 49, 49, 45);
        RecordingListener listener = new RecordingListener();
        new Battle(a, b, 0).fight(listener);

        assertTrue(listener.winner != null);
        assertTrue(a.isFainted() ^ b.isFainted(), "exactamente uno queda con HP 0");
        Pokemon winner = a.isFainted() ? b : a;
        assertEquals(winner.getDisplayName(), listener.winner);
        assertEquals("end:" + listener.winner, listener.events.get(listener.events.size() - 1));
    }

    @Test
    void losTurnosSeAlternan() throws InterruptedException {
        RecordingListener listener = new RecordingListener();
        new Battle(pokemon("a", "normal", 200, 40, 40, 80),
                pokemon("b", "normal", 200, 40, 40, 10), 0, new Random(7)).fight(listener);

        String previous = null;
        for (String event : listener.events) {
            if (event.startsWith("turn:")) {
                String attacker = event.substring(5, event.indexOf('>'));
                assertFalse(attacker.equals(previous), "un Pokémon no ataca dos veces seguidas");
                previous = attacker;
            }
        }
        assertTrue(listener.turns > 2);
    }

    @Test
    void elDanoEsAlMenosUnoAunqueLaDefensaSeaEnorme() {
        Pokemon weak = pokemon("debil", "normal", 50, 5, 5, 5);
        Pokemon wall = pokemon("muro", "normal", 50, 5, 250, 5);
        Battle battle = new Battle(weak, wall, 0, new Random(3));
        for (int i = 0; i < 100; i++) {
            wall.resetHp();
            assertTrue(battle.attack(weak, wall).damage >= 1);
        }
    }

    @Test
    void laEfectividadYElCriticoSeAplicanAlDano() {
        Random fixed = new Random() {
            @Override
            public double nextDouble() {
                return 0.0; // factores mínimos y siempre crítico (0.0 < 0.10)
            }
        };
        Pokemon squirtle = pokemon("squirtle", "water", 100, 100, 50, 40);
        Pokemon charmander = pokemon("charmander", "fire", 500, 100, 50, 60);
        Battle.AttackResult result = new Battle(squirtle, charmander, 0, fixed).attack(squirtle, charmander);

        double base = 100 * Battle.ATK_FACTOR_MIN - 50 * Battle.DEF_FACTOR_MIN;
        assertTrue(result.critical);
        assertEquals(TypeChart.SUPER_EFFECTIVE, result.modifier);
        assertEquals(Math.round(base * Battle.CRITICAL_MULTIPLIER * TypeChart.SUPER_EFFECTIVE), result.damage);
        assertEquals(500 - result.damage, charmander.getCurrentHp());
    }

    @Test
    void mismoPokemonEnAmbosLadosUsaNombresDistintos() throws InterruptedException {
        Pokemon p1 = pokemon("pikachu", "electric", 35, 55, 40, 90);
        Pokemon p2 = p1.copy();
        Battle battle = new Battle(p1, p2, 0);
        assertEquals("Pikachu (J1)", battle.getLeftName());
        assertEquals("Pikachu (J2)", battle.getRightName());

        RecordingListener listener = new RecordingListener();
        battle.fight(listener);
        assertTrue(listener.winner.endsWith("(J1)") || listener.winner.endsWith("(J2)"));
    }

    @Test
    void rechazaLaMismaInstanciaEnAmbosLados() {
        Pokemon p = pokemon("pikachu", "electric", 35, 55, 40, 90);
        assertThrows(IllegalArgumentException.class, () -> new Battle(p, p, 0));
    }

    @Test
    void elCombateRestauraElHpAlEmpezar() throws InterruptedException {
        Pokemon a = pokemon("a", "normal", 60, 50, 50, 50);
        Pokemon b = pokemon("b", "normal", 60, 50, 50, 40);
        a.applyDamage(59);
        RecordingListener listener = new RecordingListener();
        new Battle(a, b, 0, new Random(5)).fight(listener);
        assertEquals("hp:A:60", listener.events.get(1));
    }

    @Test
    void tablaDeTiposSimplificada() {
        assertEquals(1.3, TypeChart.modifier("water", "fire"));
        assertEquals(1.3, TypeChart.modifier("fire", "grass"));
        assertEquals(1.3, TypeChart.modifier("grass", "water"));
        assertEquals(0.7, TypeChart.modifier("fire", "water"));
        assertEquals(0.7, TypeChart.modifier("grass", "fire"));
        assertEquals(0.7, TypeChart.modifier("water", "grass"));
        assertEquals(1.0, TypeChart.modifier("electric", "water"));
        assertEquals(1.0, TypeChart.modifier("fire", "fire"));
    }

    @Test
    void elListenerEsObligatorio() {
        Battle battle = new Battle(pokemon("a", "normal", 1, 1, 1, 1), pokemon("b", "normal", 1, 1, 1, 1), 0);
        assertThrows(NullPointerException.class, () -> battle.fight(null));
    }

    @Test
    void chooseFirstAttackerDevuelveLaInstanciaCorrecta() {
        Pokemon a = pokemon("a", "normal", 10, 10, 10, 99);
        Pokemon b = pokemon("b", "normal", 10, 10, 10, 1);
        assertSame(a, new Battle(a, b, 0).chooseFirstAttacker());
    }
}
