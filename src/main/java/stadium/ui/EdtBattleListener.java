package stadium.ui;

import stadium.battle.BattleListener;

import javax.swing.SwingUtilities;

/**
 * Decorador que reenvía cada evento del combate al Event Dispatch Thread. Así el listener
 * de la UI puede tocar componentes Swing sin preocuparse por el hilo del combate.
 */
final class EdtBattleListener implements BattleListener {
    private final BattleListener delegate;

    EdtBattleListener(BattleListener delegate) {
        this.delegate = delegate;
    }

    @Override
    public void onBattleStarted(String firstAttacker, String secondAttacker, boolean speedTie) {
        runOnEdt(() -> delegate.onBattleStarted(firstAttacker, secondAttacker, speedTie));
    }

    @Override
    public void onTurn(String attacker, String defender, int damage, boolean critical, double modifier) {
        runOnEdt(() -> delegate.onTurn(attacker, defender, damage, critical, modifier));
    }

    @Override
    public void onHpChanged(String pokemon, int hpActual) {
        runOnEdt(() -> delegate.onHpChanged(pokemon, hpActual));
    }

    @Override
    public void onBattleEnded(String winner) {
        runOnEdt(() -> delegate.onBattleEnded(winner));
    }

    private static void runOnEdt(Runnable task) {
        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
        } else {
            SwingUtilities.invokeLater(task);
        }
    }
}
