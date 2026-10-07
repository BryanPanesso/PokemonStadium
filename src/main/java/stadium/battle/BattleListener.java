package stadium.battle;

/**
 * Eventos que emite {@link Battle}. Desacopla las reglas del combate de la interfaz:
 * la UI solo se actualiza a partir de estas notificaciones.
 *
 * <p>Los métodos se invocan desde el hilo que ejecuta el combate (no desde el EDT).
 */
public interface BattleListener {

    /**
     * Se emite una vez, antes del primer turno. Es opcional implementarlo.
     *
     * @param firstAttacker  quien ataca primero (mayor Speed)
     * @param secondAttacker su rival
     * @param speedTie       {@code true} si empataron en Speed y el inicio se sorteó
     */
    default void onBattleStarted(String firstAttacker, String secondAttacker, boolean speedTie) {
    }

    /**
     * Un ataque ya resuelto.
     *
     * @param modifier multiplicador por efectividad de tipos (1.3, 0.7 o 1.0)
     */
    void onTurn(String attacker, String defender, int damage, boolean critical, double modifier);

    /** El HP de un Pokémon cambió (también se emite al inicio con el HP completo). */
    void onHpChanged(String pokemon, int hpActual);

    /** Un HP llegó a 0. */
    void onBattleEnded(String winner);
}
