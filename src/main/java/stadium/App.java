package stadium;

import stadium.api.PokeApiClient;
import stadium.ui.BattleFrame;

import javax.swing.SwingUtilities;

/** Punto de entrada de Pokémon Stadium Lite. */
public final class App {
    private App() {
    }

    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        // Swing no es thread-safe: la ventana se crea y se muestra en el EDT.
        SwingUtilities.invokeLater(() -> new BattleFrame(new PokeApiClient()).setVisible(true));
    }
}
