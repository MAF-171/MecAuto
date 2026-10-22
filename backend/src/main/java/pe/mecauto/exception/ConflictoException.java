package pe.mecauto.exception;

/** Choca con datos existentes o con una operación concurrente (HTTP 409). Ejemplo: placa duplicada. */
public class ConflictoException extends RuntimeException {
    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}
