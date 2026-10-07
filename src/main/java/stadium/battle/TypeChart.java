package stadium.battle;

/**
 * Efectividad simplificada usando solo el primer tipo de cada Pokémon:
 * Agua &gt; Fuego, Fuego &gt; Planta, Planta &gt; Agua.
 */
public final class TypeChart {
    public static final double SUPER_EFFECTIVE = 1.3;
    public static final double NOT_VERY_EFFECTIVE = 0.7;
    public static final double NEUTRAL = 1.0;

    private TypeChart() {
    }

    public static double modifier(String attackerType, String defenderType) {
        if (beats(attackerType, defenderType)) {
            return SUPER_EFFECTIVE;
        }
        if (beats(defenderType, attackerType)) {
            return NOT_VERY_EFFECTIVE;
        }
        return NEUTRAL;
    }

    private static boolean beats(String a, String b) {
        return ("water".equals(a) && "fire".equals(b))
                || ("fire".equals(a) && "grass".equals(b))
                || ("grass".equals(a) && "water".equals(b));
    }
}
