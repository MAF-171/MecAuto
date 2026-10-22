package pe.mecauto.exception;

/** El usuario autenticado no puede hacer esta acción sobre este recurso (HTTP 403). */
public class AccesoDenegadoNegocioException extends RuntimeException {
    public AccesoDenegadoNegocioException(String mensaje) {
        super(mensaje);
    }
}
