package pe.mecauto.exception;

/** Se superó el límite de intentos permitido. Se responde con HTTP 429. */
public class DemasiadosIntentosException extends RuntimeException {
    public DemasiadosIntentosException(String mensaje) {
        super(mensaje);
    }
}
