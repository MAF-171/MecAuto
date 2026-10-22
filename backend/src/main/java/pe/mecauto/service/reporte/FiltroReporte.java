package pe.mecauto.service.reporte;

import pe.mecauto.util.Tiempo;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Filtros comunes. Si no se indican fechas se usa el mes en curso. "estado" se interpreta según el reporte. */
public record FiltroReporte(LocalDate desde, LocalDate hasta, String estado) {

    public FiltroReporte normalizado() {
        LocalDate hoy = Tiempo.hoy();
        LocalDate d = desde != null ? desde : hoy.withDayOfMonth(1);
        LocalDate h = hasta != null ? hasta : hoy;
        if (h.isBefore(d)) {
            LocalDate tmp = d;
            d = h;
            h = tmp;
        }
        String e = estado == null || estado.isBlank() ? null : estado.trim().toUpperCase();
        return new FiltroReporte(d, h, e);
    }

    public LocalDateTime inicio() {
        return desde.atStartOfDay();
    }

    /** Límite superior EXCLUSIVO: el día "hasta" se incluye completo. */
    public LocalDateTime finExclusivo() {
        return hasta.plusDays(1).atStartOfDay();
    }

    public String periodo() {
        return desde + " al " + hasta;
    }
}
