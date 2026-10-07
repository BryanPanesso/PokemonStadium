package stadium.api;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import stadium.model.Pokemon;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.text.Normalizer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

/**
 * Cliente de solo lectura para {@code https://pokeapi.co/api/v2/pokemon/{name}}.
 *
 * <p>Todas las llamadas son bloqueantes: deben ejecutarse fuera del Event Dispatch Thread
 * (la UI las lanza desde un {@code SwingWorker}). Los resultados se guardan en caché en memoria
 * para no repetir peticiones; la clase es segura para usarse desde varios hilos.
 */
public class PokeApiClient {
    public static final String BASE_URL = "https://pokeapi.co/api/v2/pokemon/";
    /** Último Pokémon de la National Dex con datos completos en PokeAPI. */
    public static final int MAX_POKEMON_ID = 1025;

    private static final Pattern VALID_NAME = Pattern.compile("[a-z0-9]+(-[a-z0-9]+)*");
    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(8);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);
    private static final String USER_AGENT = "PokemonStadiumLite/2.0 (Java HttpClient)";

    private final HttpClient http;
    private final Map<String, Pokemon> pokemonCache = new ConcurrentHashMap<>();
    private final Map<String, BufferedImage> spriteCache = new ConcurrentHashMap<>();

    public PokeApiClient() {
        this(HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build());
    }

    PokeApiClient(HttpClient http) {
        this.http = http;
    }

    /** Busca por nombre (también acepta el número de la Pokédex). */
    public Pokemon fetchByName(String rawName) throws PokeApiException {
        String key = normalizeName(rawName);
        return fetch(key, rawName.trim());
    }

    public Pokemon fetchRandom() throws PokeApiException {
        int id = ThreadLocalRandom.current().nextInt(1, MAX_POKEMON_ID + 1);
        return fetch(String.valueOf(id), "#" + id);
    }

    /**
     * Descarga el sprite como imagen. Devuelve {@code null} si el Pokémon no tiene sprite.
     */
    public BufferedImage fetchSprite(String url) throws PokeApiException {
        if (url == null || url.isBlank()) {
            return null;
        }
        BufferedImage cached = spriteCache.get(url);
        if (cached != null) {
            return cached;
        }
        HttpResponse<byte[]> response = send(request(URI.create(url)), HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() != 200) {
            throw new PokeApiException(PokeApiException.Kind.BAD_RESPONSE,
                    "No se pudo descargar el sprite (HTTP " + response.statusCode() + ").");
        }
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(response.body()));
            if (image == null) {
                throw new PokeApiException(PokeApiException.Kind.BAD_RESPONSE, "Formato de sprite no soportado.");
            }
            spriteCache.put(url, image);
            return image;
        } catch (IOException e) {
            throw new PokeApiException(PokeApiException.Kind.BAD_RESPONSE, "El sprite descargado está dañado.", e);
        }
    }

    private Pokemon fetch(String key, String shownInput) throws PokeApiException {
        Pokemon cached = pokemonCache.get(key);
        if (cached != null) {
            return cached.copy();
        }

        HttpResponse<String> response = send(request(URI.create(BASE_URL + key)), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 404) {
            throw new PokeApiException(PokeApiException.Kind.NOT_FOUND, "Pokémon no encontrado: \"" + shownInput + "\".");
        }
        if (response.statusCode() != 200) {
            throw new PokeApiException(PokeApiException.Kind.BAD_RESPONSE,
                    "PokeAPI respondió con un error (HTTP " + response.statusCode() + "). Intenta de nuevo.");
        }

        Pokemon pokemon = parsePokemon(response.body());
        pokemonCache.put(key, pokemon);
        pokemonCache.put(pokemon.getName(), pokemon);
        pokemonCache.put(String.valueOf(pokemon.getId()), pokemon);
        return pokemon.copy();
    }

    private HttpRequest request(URI uri) {
        return HttpRequest.newBuilder(uri)
                .timeout(REQUEST_TIMEOUT)
                .header("User-Agent", USER_AGENT)
                .GET()
                .build();
    }

    /** Envía la petición traduciendo los fallos de red a mensajes comprensibles. */
    private <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> handler) throws PokeApiException {
        try {
            return http.send(request, handler);
        } catch (HttpTimeoutException e) {
            throw new PokeApiException(PokeApiException.Kind.NETWORK,
                    "Error de red: PokeAPI tardó demasiado en responder.", e);
        } catch (IOException e) {
            throw new PokeApiException(PokeApiException.Kind.NETWORK,
                    "Error de red: no se pudo conectar con PokeAPI. Revisa tu conexión a internet.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PokeApiException(PokeApiException.Kind.NETWORK, "La consulta fue cancelada.", e);
        }
    }

    /**
     * Convierte lo que escribe el usuario al formato de PokeAPI: "  Mr Mime " pasa a "mr-mime",
     * "Flabébé" a "flabebe". Valida antes de hacer la petición para dar un error inmediato.
     */
    static String normalizeName(String raw) throws PokeApiException {
        if (raw == null || raw.isBlank()) {
            throw new PokeApiException(PokeApiException.Kind.INVALID_INPUT, "Escribe el nombre o número de un Pokémon.");
        }
        String name = Normalizer.normalize(raw.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        name = DIACRITICS.matcher(name).replaceAll("")
                .replaceAll("[\\s_.]+", "-")
                .replaceAll("['’]", "");
        if (!VALID_NAME.matcher(name).matches()) {
            throw new PokeApiException(PokeApiException.Kind.INVALID_INPUT,
                    "Nombre inválido: usa solo letras, números y guiones.");
        }
        return name;
    }

    static Pokemon parsePokemon(String body) throws PokeApiException {
        try {
            JSONObject json = new JSONObject(body);
            JSONArray stats = json.getJSONArray("stats");
            return new Pokemon(
                    json.getInt("id"),
                    json.getString("name"),
                    parseSpriteUrl(json.getJSONObject("sprites")),
                    parseTypes(json.getJSONArray("types")),
                    baseStat(stats, "hp"),
                    baseStat(stats, "attack"),
                    baseStat(stats, "defense"),
                    baseStat(stats, "speed"));
        } catch (JSONException | IllegalArgumentException e) {
            throw new PokeApiException(PokeApiException.Kind.BAD_RESPONSE, "PokeAPI devolvió datos incompletos.", e);
        }
    }

    /** Los tipos vienen con un campo "slot"; se ordenan por él para que el primero sea el principal. */
    private static List<String> parseTypes(JSONArray typesArray) {
        List<JSONObject> slots = new ArrayList<>();
        for (int i = 0; i < typesArray.length(); i++) {
            slots.add(typesArray.getJSONObject(i));
        }
        slots.sort((a, b) -> Integer.compare(a.optInt("slot"), b.optInt("slot")));
        List<String> types = new ArrayList<>();
        for (JSONObject slot : slots) {
            types.add(slot.getJSONObject("type").getString("name"));
        }
        return types;
    }

    private static int baseStat(JSONArray stats, String statName) {
        for (int i = 0; i < stats.length(); i++) {
            JSONObject stat = stats.getJSONObject(i);
            if (statName.equals(stat.getJSONObject("stat").getString("name"))) {
                return stat.getInt("base_stat");
            }
        }
        throw new JSONException("Falta el stat " + statName);
    }

    /** Sprite frontal; si no existe se usa el artwork oficial. */
    private static String parseSpriteUrl(JSONObject sprites) {
        String front = sprites.optString("front_default", "");
        if (!front.isBlank() && !"null".equals(front)) {
            return front;
        }
        JSONObject other = sprites.optJSONObject("other");
        JSONObject artwork = other == null ? null : other.optJSONObject("official-artwork");
        String official = artwork == null ? "" : artwork.optString("front_default", "");
        return "null".equals(official) ? "" : official;
    }
}
