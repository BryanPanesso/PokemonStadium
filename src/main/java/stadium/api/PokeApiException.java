package stadium.api;

/**
 * Error al consultar PokeAPI. El mensaje está pensado para mostrarse tal cual al usuario
 * y {@link Kind} permite a la UI reaccionar distinto según la causa.
 */
public class PokeApiException extends Exception {

    public enum Kind {
        /** El texto ingresado no es un nombre válido; no se llegó a hacer la petición. */
        INVALID_INPUT,
        /** PokeAPI respondió 404. */
        NOT_FOUND,
        /** Sin conexión, DNS, timeout o petición cancelada. */
        NETWORK,
        /** Respuesta inesperada: código HTTP distinto de 200, JSON o imagen inválidos. */
        BAD_RESPONSE
    }

    private final Kind kind;

    public PokeApiException(Kind kind, String message) {
        super(message);
        this.kind = kind;
    }

    public PokeApiException(Kind kind, String message, Throwable cause) {
        super(message, cause);
        this.kind = kind;
    }

    public Kind getKind() {
        return kind;
    }
}
