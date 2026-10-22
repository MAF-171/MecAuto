package pe.mecauto.exception;

/** El registro pedido no existe (HTTP 404). */
public class RecursoNoEncontradoException extends RuntimeException {
    public RecursoNoEncontradoException(String recurso, Object id) {
        super(recurso + " no encontrado (id " + id + ")");
    }

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
