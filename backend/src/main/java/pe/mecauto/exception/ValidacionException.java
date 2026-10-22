package pe.mecauto.exception;

import java.util.Map;

/** Datos inválidos que solo se detectan en el servicio (HTTP 400), indicando el campo del formulario. */
public class ValidacionException extends RuntimeException {

    private final Map<String, String> campos;

    public ValidacionException(String campo, String mensaje) {
        super(mensaje);
        this.campos = Map.of(campo, mensaje);
    }

    public Map<String, String> getCampos() {
        return campos;
    }
}
