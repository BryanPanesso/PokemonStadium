package stadium.model;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Modelo de un Pokémon: datos obtenidos de PokeAPI (stats base, tipos y sprite)
 * más su único estado mutable, el HP actual durante un combate.
 */
public class Pokemon {
    private final int id;
    private final String name;
    private final String spriteUrl;
    private final List<String> types;
    private final int maxHp;
    private final int attack;
    private final int defense;
    private final int speed;

    // Lo modifica el hilo del combate; volatile garantiza que cualquier hilo lea el último valor.
    private volatile int currentHp;

    public Pokemon(int id, String name, String spriteUrl, List<String> types,
                   int maxHp, int attack, int defense, int speed) {
        if (maxHp <= 0) {
            throw new IllegalArgumentException("El HP máximo debe ser positivo: " + maxHp);
        }
        if (attack < 0 || defense < 0 || speed < 0) {
            throw new IllegalArgumentException("Los stats no pueden ser negativos.");
        }
        this.id = id;
        this.name = Objects.requireNonNull(name, "name");
        this.spriteUrl = spriteUrl == null ? "" : spriteUrl;
        this.types = List.copyOf(Objects.requireNonNull(types, "types"));
        this.maxHp = maxHp;
        this.attack = attack;
        this.defense = defense;
        this.speed = speed;
        this.currentHp = maxHp;
    }

    /** Copia con HP completo; cada jugador necesita su propia instancia aunque elijan el mismo Pokémon. */
    public Pokemon copy() {
        return new Pokemon(id, name, spriteUrl, types, maxHp, attack, defense, speed);
    }

    public int getId() {
        return id;
    }

    /** Nombre tal como lo entrega PokeAPI (minúsculas, con guiones). */
    public String getName() {
        return name;
    }

    /** Nombre legible para mostrar: "mr-mime" pasa a "Mr-Mime". */
    public String getDisplayName() {
        StringBuilder sb = new StringBuilder(name.length());
        boolean capitalizeNext = true;
        for (char c : name.toCharArray()) {
            sb.append(capitalizeNext ? Character.toUpperCase(c) : c);
            capitalizeNext = c == '-' || c == ' ';
        }
        return sb.toString();
    }

    public String getSpriteUrl() {
        return spriteUrl;
    }

    public List<String> getTypes() {
        return types;
    }

    public String getPrimaryType() {
        return types.isEmpty() ? "normal" : types.get(0).toLowerCase(Locale.ROOT);
    }

    public int getMaxHp() {
        return maxHp;
    }

    public int getAttack() {
        return attack;
    }

    public int getDefense() {
        return defense;
    }

    public int getSpeed() {
        return speed;
    }

    public int getCurrentHp() {
        return currentHp;
    }

    public void resetHp() {
        currentHp = maxHp;
    }

    /** Aplica daño sin permitir que el HP sea negativo. */
    public void applyDamage(int damage) {
        currentHp = Math.max(0, currentHp - Math.max(0, damage));
    }

    public boolean isFainted() {
        return currentHp == 0;
    }

    @Override
    public String toString() {
        return getDisplayName() + " #" + id + " " + types + " HP " + currentHp + "/" + maxHp;
    }
}
