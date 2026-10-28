package pe.mecauto.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.mecauto.domain.EstadoCita;
import pe.mecauto.domain.FranjasHorarias;
import pe.mecauto.domain.OrigenCita;
import pe.mecauto.dto.CitaDtos.*;
import pe.mecauto.entity.Cita;
import pe.mecauto.entity.Cliente;
import pe.mecauto.entity.Servicio;
import pe.mecauto.entity.Vehiculo;
import pe.mecauto.exception.ConflictoException;
import pe.mecauto.exception.NegocioException;
import pe.mecauto.exception.ValidacionException;
import pe.mecauto.repository.CitaRepository;
import pe.mecauto.repository.ClienteRepository;
import pe.mecauto.repository.ServicioRepository;
import pe.mecauto.repository.VehiculoRepository;
import pe.mecauto.util.Tiempo;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Reglas de citas con repositorios simulados (no requiere SQL Server).
 * Horario de prueba: todos los días de 08:00 a 18:00, franjas de 60 minutos y 2 citas por franja.
 */
@ExtendWith(MockitoExtension.class)
class CitaServiceTest {

    @Mock CitaRepository citas;
    @Mock ClienteRepository clientes;
    @Mock VehiculoRepository vehiculos;
    @Mock ServicioRepository servicios;
    @Mock ConfiguracionService config;
    @Mock SecuenciaService secuencias;

    CitaService service;
    Servicio cambioAceite;
    LocalDate manana;

    @BeforeEach
    void preparar() {
        service = new CitaService(citas, clientes, vehiculos, servicios, config, secuencias);
        lenient().when(config.franjas()).thenReturn(new FranjasHorarias(LocalTime.of(8, 0), LocalTime.of(18, 0), 60,
                EnumSet.allOf(DayOfWeek.class)));
        lenient().when(config.citasPorFranja()).thenReturn(2);
        lenient().when(secuencias.siguienteCodigo("CITA")).thenReturn("CITA-0001");
        lenient().when(citas.save(any(Cita.class))).thenAnswer(i -> i.getArgument(0));
        cambioAceite = new Servicio();
        cambioAceite.setId(1L);
        cambioAceite.setNombre("Cambio de aceite");
        lenient().when(servicios.findById(1L)).thenReturn(Optional.of(cambioAceite));
        manana = Tiempo.hoy().plusDays(1);
    }

    private CitaRequest req(LocalDateTime fechaHora, String placa) {
        return new CitaRequest(null, null, 1L, " Juan Pérez ", " 987654321 ", "", placa, "Toyota", "", fechaHora, null);
    }

    private Cita cita(long id, EstadoCita estado, LocalDateTime fecha) {
        Cita c = new Cita();
        c.setId(id);
        c.setCodigo("CITA-000" + id);
        c.setServicio(cambioAceite);
        c.setPlaca("ABC-123");
        c.setFechaHora(fecha);
        c.setEstado(estado);
        return c;
    }

    @Test
    @DisplayName("Crear guarda la cita al inicio de su franja, con placa normalizada y estado pendiente")
    void crearAjustaAFranja() {
        CitaDto c = service.crear(req(manana.atTime(10, 25), "abc 123"));

        assertEquals("CITA-0001", c.codigo());
        assertEquals(manana.atTime(10, 0), c.fechaHora());
        assertEquals("ABC123", c.placa());
        assertEquals("Juan Pérez", c.nombreContacto());
        assertNull(c.correoContacto());
        assertEquals(EstadoCita.PENDIENTE, c.estado());
        assertEquals(OrigenCita.INTERNA, c.origen());
    }

    @Test
    @DisplayName("Fuera del horario de atención no se agenda")
    void fueraDeHorario() {
        ValidacionException e = assertThrows(ValidacionException.class, () -> service.crear(req(manana.atTime(19, 0), "ABC-123")));
        assertTrue(e.getCampos().containsKey("fechaHora"));
    }

    @Test
    @DisplayName("No se agenda en el pasado ni a más de 90 días")
    void fechasNoPermitidas() {
        assertThrows(ValidacionException.class, () -> service.crear(req(Tiempo.hoy().minusDays(1).atTime(10, 0), "ABC-123")));
        assertThrows(ValidacionException.class, () -> service.crear(req(Tiempo.hoy().plusDays(91).atTime(10, 0), "ABC-123")));
    }

    @Test
    @DisplayName("Una franja sin cupos rechaza la cita")
    void sinCupos() {
        when(citas.countByFechaHoraGreaterThanEqualAndFechaHoraLessThanAndEstadoIn(any(), any(), any())).thenReturn(2L);
        assertThrows(ConflictoException.class, () -> service.crear(req(manana.atTime(10, 0), "ABC-123")));
    }

    @Test
    @DisplayName("La misma placa no puede tener dos citas abiertas el mismo día")
    void placaRepetidaElMismoDia() {
        when(citas.existsByPlacaAndFechaHoraGreaterThanEqualAndFechaHoraLessThanAndEstadoIn(eq("ABC-123"), any(), any(), any()))
                .thenReturn(true);
        assertThrows(ConflictoException.class, () -> service.crear(req(manana.atTime(10, 0), "ABC-123")));
    }

    @Test
    @DisplayName("Un servicio desactivado no se puede reservar; una placa inválida tampoco")
    void servicioInactivoOPlacaInvalida() {
        assertThrows(ValidacionException.class, () -> service.crear(req(manana.atTime(10, 0), "X")));
        cambioAceite.setActivo(false);
        assertThrows(NegocioException.class, () -> service.crear(req(manana.atTime(10, 0), "ABC-123")));
    }

    @Test
    @DisplayName("El vehículo debe pertenecer al cliente elegido")
    void vehiculoDeOtroCliente() {
        Cliente juan = new Cliente();
        juan.setId(1L);
        Cliente ana = new Cliente();
        ana.setId(2L);
        Vehiculo deAna = new Vehiculo();
        deAna.setId(5L);
        deAna.setCliente(ana);
        when(clientes.findById(1L)).thenReturn(Optional.of(juan));
        when(vehiculos.findById(5L)).thenReturn(Optional.of(deAna));

        CitaRequest r = new CitaRequest(1L, 5L, 1L, "Juan", "987654321", null, "ABC-123", null, null, manana.atTime(10, 0), null);
        assertThrows(ValidacionException.class, () -> service.crear(r));
    }

    @Test
    @DisplayName("Cita pública: si la placa ya existe se vincula con su cliente y vehículo")
    void publicaVinculaVehiculo() {
        Cliente juan = new Cliente();
        juan.setId(1L);
        Vehiculo v = new Vehiculo();
        v.setId(5L);
        v.setCliente(juan);
        when(vehiculos.findByPlaca("ABC-123")).thenReturn(Optional.of(v));
        CitaPublicaRequest r = new CitaPublicaRequest("Juan", "987654321", null, "abc-123", null, null, 1L,
                manana.atTime(9, 0), null, null);

        CitaPublicaResponse resp = service.crearPublica(r);

        assertEquals("CITA-0001", resp.codigo());
        verify(citas).save(argThat(c -> c.getVehiculo() == v && c.getCliente() == juan && c.getOrigen() == OrigenCita.PUBLICA));
    }

    @Test
    @DisplayName("Cita pública con el campo trampa lleno (robot) no se guarda")
    void campoTrampa() {
        CitaPublicaRequest r = new CitaPublicaRequest("Bot", "987654321", null, "ABC-123", null, null, 1L,
                manana.atTime(9, 0), null, "http://spam.example");

        service.crearPublica(r);

        verify(citas, never()).save(any());
    }

    @Test
    @DisplayName("Solo una cita pendiente se confirma; una cancelada ya no se modifica")
    void transicionesDeEstado() {
        Cita pendiente = cita(1L, EstadoCita.PENDIENTE, manana.atTime(10, 0));
        Cita cancelada = cita(2L, EstadoCita.CANCELADA, manana.atTime(11, 0));
        when(citas.findById(1L)).thenReturn(Optional.of(pendiente));
        when(citas.findById(2L)).thenReturn(Optional.of(cancelada));

        assertEquals(EstadoCita.CONFIRMADA, service.confirmar(1L).estado());
        assertThrows(NegocioException.class, () -> service.confirmar(1L));
        assertThrows(NegocioException.class, () -> service.cancelar(2L));
        assertThrows(NegocioException.class, () -> service.reprogramar(2L, new ReprogramarRequest(manana.atTime(12, 0))));
        assertEquals(EstadoCita.CANCELADA, service.cancelar(1L).estado());
    }

    @Test
    @DisplayName("Reprogramar mueve la cita a la nueva franja")
    void reprogramar() {
        Cita c = cita(1L, EstadoCita.CONFIRMADA, manana.atTime(10, 0));
        when(citas.findById(1L)).thenReturn(Optional.of(c));
        // la propia cita ya ocupa ese día: al reprogramar dentro del mismo día no cuenta como placa repetida
        lenient().when(citas.existsByPlacaAndFechaHoraGreaterThanEqualAndFechaHoraLessThanAndEstadoIn(any(), any(), any(), any()))
                .thenReturn(true);

        assertEquals(manana.atTime(15, 0), service.reprogramar(1L, new ReprogramarRequest(manana.atTime(15, 40))).fechaHora());
    }

    @Test
    @DisplayName("Disponibilidad: descuenta las citas ocupadas de cada franja")
    void disponibilidad() {
        List<Object[]> ocupacion = List.<Object[]>of(new Object[]{manana.atTime(8, 0), 2L}, new Object[]{manana.atTime(9, 0), 1L});
        when(citas.ocupacionPorFranja(any(), any(), any())).thenReturn(ocupacion);

        DisponibilidadDto d = service.disponibilidad(manana);

        assertTrue(d.laborable());
        assertEquals(10, d.franjas().size());
        assertEquals(new FranjaDto("08:00", 0), d.franjas().get(0));
        assertEquals(new FranjaDto("09:00", 1), d.franjas().get(1));
        assertEquals(new FranjaDto("10:00", 2), d.franjas().get(2));
    }

    @Test
    @DisplayName("Un día no laborable no tiene franjas")
    void diaNoLaborable() {
        when(config.franjas()).thenReturn(new FranjasHorarias(LocalTime.of(8, 0), LocalTime.of(18, 0), 60,
                EnumSet.complementOf(EnumSet.of(manana.getDayOfWeek()))));

        DisponibilidadDto d = service.disponibilidad(manana);

        assertFalse(d.laborable());
        assertTrue(d.franjas().isEmpty());
    }
}
