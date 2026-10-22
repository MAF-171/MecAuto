package pe.mecauto.exception;

import java.time.OffsetDateTime;
import java.util.Map;

/** Formato único de error de la API. */
public record ApiError(
        OffsetDateTime fecha,
        int estado,
        String error,
        String mensaje,
        String ruta,
        Map<String, String> campos) {
}
