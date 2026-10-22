package pe.mecauto.exception;

/** Se violó una regla de negocio (HTTP 422). Ejemplo: stock insuficiente, transición de estado inválida. */
public class NegocioException extends RuntimeException {
    public NegocioException(String mensaje) {
        super(mensaje);
    }
}
