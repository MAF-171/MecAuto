package pe.mecauto.domain;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Reglas de horario de atención para las citas. Sin dependencias de Spring.
 * Las franjas se generan desde la hora de apertura hasta la de cierre con una duración fija.
 */
public final class FranjasHorarias {

    private final LocalTime apertura;
    private final LocalTime cierre;
    private final int minutosPorFranja;
    private final Set<DayOfWeek> diasLaborables;

    public FranjasHorarias(LocalTime apertura, LocalTime cierre, int minutosPorFranja, Set<DayOfWeek> diasLaborables) {
        if (minutosPorFranja <= 0 || !apertura.isBefore(cierre)) {
            throw new IllegalArgumentException("Horario de atención inválido");
        }
        this.apertura = apertura;
        this.cierre = cierre;
        this.minutosPorFranja = minutosPorFranja;
        this.diasLaborables = diasLaborables;
    }

    public boolean esDiaLaborable(LocalDate fecha) {
        return diasLaborables.contains(fecha.getDayOfWeek());
    }

    /** Horas de inicio de cada franja de un día (vacío si no es laborable). */
    public List<LocalTime> franjasDelDia(LocalDate fecha) {
        List<LocalTime> franjas = new ArrayList<>();
        if (!esDiaLaborable(fecha)) {
            return franjas;
        }
        LocalTime t = apertura;
        while (!t.plusMinutes(minutosPorFranja).isAfter(cierre)) {
            franjas.add(t);
            t = t.plusMinutes(minutosPorFranja);
        }
        return franjas;
    }

    /** Devuelve el inicio de la franja a la que pertenece la fecha/hora, o null si cae fuera del horario. */
    public LocalDateTime inicioDeFranja(LocalDateTime fechaHora) {
        LocalDate fecha = fechaHora.toLocalDate();
        LocalTime hora = fechaHora.toLocalTime();
        for (LocalTime inicio : franjasDelDia(fecha)) {
            if (!hora.isBefore(inicio) && hora.isBefore(inicio.plusMinutes(minutosPorFranja))) {
                return LocalDateTime.of(fecha, inicio);
            }
        }
        return null;
    }

    public int minutosPorFranja() {
        return minutosPorFranja;
    }
}
