package stadium.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PokemonTest {

    private static Pokemon pikachu() {
        return new Pokemon(25, "pikachu", "url", List.of("electric"), 35, 55, 40, 90);
    }

    @Test
    void elHpNoBajaDeCero() {
        Pokemon p = pikachu();
        p.applyDamage(1000);
        assertEquals(0, p.getCurrentHp());
        assertTrue(p.isFainted());
    }

    @Test
    void elDanoNegativoSeIgnora() {
        Pokemon p = pikachu();
        p.applyDamage(-10);
        assertEquals(35, p.getCurrentHp());
    }

    @Test
    void copyEsIndependienteYConHpCompleto() {
        Pokemon p = pikachu();
        p.applyDamage(20);
        Pokemon copy = p.copy();
        assertNotSame(p, copy);
        assertEquals(35, copy.getCurrentHp());
        assertEquals(15, p.getCurrentHp());
    }

    @Test
    void nombreLegible() {
        assertEquals("Mr-Mime", new Pokemon(122, "mr-mime", "", List.of("psychic"), 40, 45, 65, 90).getDisplayName());
        assertEquals("Pikachu", pikachu().getDisplayName());
    }

    @Test
    void losTiposSonInmutables() {
        List<String> types = new ArrayList<>(List.of("fire"));
        Pokemon p = new Pokemon(4, "charmander", "", types, 39, 52, 43, 65);
        types.add("dragon");
        assertEquals(List.of("fire"), p.getTypes());
        assertThrows(UnsupportedOperationException.class, () -> p.getTypes().add("x"));
    }

    @Test
    void validaElHpMaximo() {
        assertThrows(IllegalArgumentException.class,
                () -> new Pokemon(1, "x", "", List.of(), 0, 1, 1, 1));
    }

    @Test
    void sinTiposElPrincipalEsNormal() {
        assertEquals("normal", new Pokemon(1, "x", "", List.of(), 1, 1, 1, 1).getPrimaryType());
    }
}
