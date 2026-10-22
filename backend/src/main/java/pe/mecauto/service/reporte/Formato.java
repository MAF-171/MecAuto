package pe.mecauto.service.reporte;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Formatos de fecha para mostrar en reportes. */
final class Formato {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private Formato() {
    }

    static String fecha(LocalDate f) {
        return f == null ? "" : f.format(FECHA);
    }

    static String fechaHora(LocalDateTime f) {
        return f == null ? "" : f.format(FECHA_HORA);
    }

    static String texto(Object o) {
        return o == null ? "" : o.toString();
    }
}
