package pe.mecauto.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.*;

class FranjasHorariasTest {

    // Lunes a sábado, 08:00-18:00, franjas de 60 min
    private final FranjasHorarias f = new FranjasHorarias(LocalTime.of(8, 0), LocalTime.of(18, 0), 60,
            EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.SATURDAY));
    private static final LocalDate LUNES = LocalDate.of(2026, 10, 19);
    private static final LocalDate DOMINGO = LocalDate.of(2026, 10, 18);

    @Test
    @DisplayName("Un día laborable tiene 10 franjas de 08:00 a 17:00")
    void franjasDeUnDia() {
        var franjas = f.franjasDelDia(LUNES);
        assertEquals(10, franjas.size());
        assertEquals(LocalTime.of(8, 0), franjas.get(0));
        assertEquals(LocalTime.of(17, 0), franjas.get(9));
    }

    @Test
    @DisplayName("El domingo no tiene franjas")
    void domingoCerrado() {
        assertFalse(f.esDiaLaborable(DOMINGO));
        assertTrue(f.franjasDelDia(DOMINGO).isEmpty());
    }

    @Test
    @DisplayName("Una hora intermedia se normaliza al inicio de su franja")
    void normalizaHora() {
        assertEquals(LocalDateTime.of(LUNES, LocalTime.of(10, 0)), f.inicioDeFranja(LocalDateTime.of(LUNES, LocalTime.of(10, 45))));
    }

    @Test
    @DisplayName("Fuera del horario no hay franja")
    void fueraDeHorario() {
        assertNull(f.inicioDeFranja(LocalDateTime.of(LUNES, LocalTime.of(7, 59))));
        assertNull(f.inicioDeFranja(LocalDateTime.of(LUNES, LocalTime.of(18, 0))));
        assertNull(f.inicioDeFranja(LocalDateTime.of(DOMINGO, LocalTime.of(10, 0))));
    }

    @Test
    @DisplayName("Horario inválido es rechazado")
    void horarioInvalido() {
        assertThrows(IllegalArgumentException.class,
                () -> new FranjasHorarias(LocalTime.of(18, 0), LocalTime.of(8, 0), 60, EnumSet.of(DayOfWeek.MONDAY)));
        assertThrows(IllegalArgumentException.class,
                () -> new FranjasHorarias(LocalTime.of(8, 0), LocalTime.of(18, 0), 0, EnumSet.of(DayOfWeek.MONDAY)));
    }
}
