package pe.mecauto.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Fecha y hora del taller. Se usa siempre la zona horaria de Lima para que los
 * registros no dependan de la zona configurada en el servidor.
 */
public final class Tiempo {

    public static final ZoneId ZONA = ZoneId.of("America/Lima");

    private Tiempo() {
    }

    public static LocalDateTime ahora() {
        return LocalDateTime.now(ZONA);
    }

    public static LocalDate hoy() {
        return LocalDate.now(ZONA);
    }
}
