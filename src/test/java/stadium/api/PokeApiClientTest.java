package stadium.api;

import org.junit.jupiter.api.Test;
import stadium.model.Pokemon;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Pruebas sin red: normalización de nombres y parseo de un JSON con la forma real de PokeAPI. */
class PokeApiClientTest {

    private static final String BULBASAUR_JSON = "{"
            + "\"id\": 1, \"name\": \"bulbasaur\","
            + "\"sprites\": {\"front_default\": \"https://example.org/1.png\","
            + "  \"other\": {\"official-artwork\": {\"front_default\": \"https://example.org/art/1.png\"}}},"
            + "\"types\": ["
            + "  {\"slot\": 2, \"type\": {\"name\": \"poison\"}},"
            + "  {\"slot\": 1, \"type\": {\"name\": \"grass\"}}],"
            + "\"stats\": ["
            + "  {\"base_stat\": 45, \"stat\": {\"name\": \"hp\"}},"
            + "  {\"base_stat\": 49, \"stat\": {\"name\": \"attack\"}},"
            + "  {\"base_stat\": 49, \"stat\": {\"name\": \"defense\"}},"
            + "  {\"base_stat\": 65, \"stat\": {\"name\": \"special-attack\"}},"
            + "  {\"base_stat\": 65, \"stat\": {\"name\": \"special-defense\"}},"
            + "  {\"base_stat\": 45, \"stat\": {\"name\": \"speed\"}}]"
            + "}";

    @Test
    void parseaLosCamposNecesarios() throws PokeApiException {
        Pokemon p = PokeApiClient.parsePokemon(BULBASAUR_JSON);
        assertEquals(1, p.getId());
        assertEquals("bulbasaur", p.getName());
        assertEquals(List.of("grass", "poison"), p.getTypes(), "ordenados por slot");
        assertEquals("grass", p.getPrimaryType());
        assertEquals(45, p.getMaxHp());
        assertEquals(49, p.getAttack());
        assertEquals(49, p.getDefense());
        assertEquals(45, p.getSpeed());
        assertEquals(45, p.getCurrentHp());
        assertEquals("https://example.org/1.png", p.getSpriteUrl());
    }

    @Test
    void sinSpriteFrontalUsaElArtworkOficial() throws PokeApiException {
        String json = BULBASAUR_JSON.replace("\"front_default\": \"https://example.org/1.png\"", "\"front_default\": null");
        assertEquals("https://example.org/art/1.png", PokeApiClient.parsePokemon(json).getSpriteUrl());
    }

    @Test
    void jsonIncompletoEsBadResponse() {
        PokeApiException e = assertThrows(PokeApiException.class,
                () -> PokeApiClient.parsePokemon("{\"id\": 1, \"name\": \"x\"}"));
        assertEquals(PokeApiException.Kind.BAD_RESPONSE, e.getKind());
    }

    @Test
    void jsonInvalidoEsBadResponse() {
        PokeApiException e = assertThrows(PokeApiException.class, () -> PokeApiClient.parsePokemon("<html>"));
        assertEquals(PokeApiException.Kind.BAD_RESPONSE, e.getKind());
    }

    @Test
    void normalizaLoQueEscribeElUsuario() throws PokeApiException {
        assertEquals("pikachu", PokeApiClient.normalizeName("  Pikachu "));
        assertEquals("mr-mime", PokeApiClient.normalizeName("Mr Mime"));
        assertEquals("mr-mime", PokeApiClient.normalizeName("Mr. Mime"));
        assertEquals("flabebe", PokeApiClient.normalizeName("Flabébé"));
        assertEquals("farfetchd", PokeApiClient.normalizeName("Farfetch'd"));
        assertEquals("25", PokeApiClient.normalizeName("25"));
    }

    @Test
    void rechazaEntradasVaciasOInvalidas() {
        for (String input : new String[]{null, "", "   ", "pika/chu", "../etc", "pikachu?x=1", "-"}) {
            PokeApiException e = assertThrows(PokeApiException.class, () -> PokeApiClient.normalizeName(input),
                    "debería rechazar: " + input);
            assertEquals(PokeApiException.Kind.INVALID_INPUT, e.getKind());
        }
    }
}
