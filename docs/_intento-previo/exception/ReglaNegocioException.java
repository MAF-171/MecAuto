package pe.mecauto.exception;

/** Se incumple una regla del negocio (estado inválido, stock insuficiente, etc.). Se responde con HTTP 422. */
public class ReglaNegocioException extends RuntimeException {
    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
