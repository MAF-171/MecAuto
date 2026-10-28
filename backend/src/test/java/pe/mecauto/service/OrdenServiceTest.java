package pe.mecauto.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.mecauto.domain.*;
import pe.mecauto.dto.OrdenDtos.*;
import pe.mecauto.entity.*;
import pe.mecauto.exception.AccesoDenegadoNegocioException;
import pe.mecauto.exception.ConflictoException;
import pe.mecauto.exception.NegocioException;
import pe.mecauto.exception.ValidacionException;
import pe.mecauto.repository.*;
import pe.mecauto.security.UsuarioPrincipal;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Órdenes de trabajo: creación, permisos por rol, flujo de estados (RN02) y descuento de inventario,
 * con repositorios simulados (no requiere SQL Server).
 */
@ExtendWith(MockitoExtension.class)
class OrdenServiceTest {

    @Mock OrdenTrabajoRepository ordenes;
    @Mock OrdenHistorialRepository historiales;
    @Mock VehiculoRepository vehiculos;
    @Mock UsuarioRepository usuarios;
    @Mock ServicioRepository servicios;
    @Mock RepuestoRepository repuestos;
    @Mock CitaRepository citas;
    @Mock ComprobanteRepository comprobantes;
    @Mock InventarioService inventario;
    @Mock SecuenciaService secuencias;

    OrdenService service;
    Vehiculo auto;
    Usuario mecanico;
    UsuarioPrincipal admin;
    UsuarioPrincipal recepcion;
    UsuarioPrincipal mecanicoActor;
    UsuarioPrincipal otroMecanico;
    Repuesto filtro;

    @BeforeEach
    void preparar() {
        service = new OrdenService(ordenes, historiales, vehiculos, usuarios, servicios, repuestos, citas,
                comprobantes, inventario, secuencias);
        Cliente juan = new Cliente();
        juan.setId(1L);
        juan.setNombre("Juan Pérez");
        auto = new Vehiculo();
        auto.setId(5L);
        auto.setCliente(juan);
        auto.setPlaca("ABC-123");
        auto.setMarca("Toyota");
        auto.setModelo("Corolla");
        auto.setKilometraje(40000);
        lenient().when(vehiculos.findById(5L)).thenReturn(Optional.of(auto));

        mecanico = usuario(20L, Rol.MECANICO);
        lenient().when(usuarios.findById(20L)).thenReturn(Optional.of(mecanico));
        admin = new UsuarioPrincipal(usuario(1L, Rol.ADMINISTRADOR));
        recepcion = new UsuarioPrincipal(usuario(2L, Rol.RECEPCION));
        mecanicoActor = new UsuarioPrincipal(mecanico);
        otroMecanico = new UsuarioPrincipal(usuario(21L, Rol.MECANICO));
        lenient().when(usuarios.getReferenceById(anyLong())).thenAnswer(i -> usuario(i.getArgument(0), Rol.RECEPCION));
        lenient().when(secuencias.siguienteCodigo("OT")).thenReturn("OT-0001");
        lenient().when(comprobantes.findFirstByOrdenIdAndEstado(any(), any())).thenReturn(Optional.empty());

        filtro = new Repuesto();
        filtro.setId(30L);
        filtro.setCodigo("FIL-01");
        filtro.setNombre("Filtro de aceite");
        filtro.setPrecioVenta(new BigDecimal("28.00"));
        filtro.setStockActual(5);
        lenient().when(repuestos.findById(30L)).thenReturn(Optional.of(filtro));
    }

    private static Usuario usuario(long id, Rol rol) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setUsername("u" + id);
        u.setNombre("Usuario " + id);
        u.setRol(rol);
        return u;
    }

    /** Orden guardada en el estado indicado, asignada al mecánico 20. */
    private OrdenTrabajo orden(EstadoOrden estado) {
        OrdenTrabajo o = new OrdenTrabajo();
        o.setId(9L);
        o.setCodigo("OT-0009");
        o.setCliente(auto.getCliente());
        o.setVehiculo(auto);
        o.setMecanico(mecanico);
        o.setFalla("Ruido al frenar");
        o.setEstado(estado);
        lenient().when(ordenes.findByIdForUpdate(9L)).thenReturn(Optional.of(o));
        lenient().when(ordenes.findById(9L)).thenReturn(Optional.of(o));
        return o;
    }

    private DetalleOrdenRepuesto lineaFiltro(OrdenTrabajo o, int cantidad) {
        DetalleOrdenRepuesto d = new DetalleOrdenRepuesto();
        d.setId(70L);
        d.setOrden(o);
        d.setRepuesto(filtro);
        d.setCantidad(cantidad);
        d.setPrecioUnitario(new BigDecimal("28.00"));
        o.getRepuestos().add(d);
        o.recalcularTotal();
        return d;
    }

    private OrdenDetalleDto pasarA(EstadoOrden destino, UsuarioPrincipal actor) {
        return service.cambiarEstado(9L, new CambioEstadoRequest(destino, null), actor);
    }

    // ------------------------------------------------------------------ Creación

    @Test
    @DisplayName("Crear deja la orden RECIBIDA, actualiza el kilometraje y registra el historial")
    void crear() {
        OrdenDetalleDto o = service.crear(new OrdenCrearRequest(5L, " Ruido al frenar ", 20L, 41000, null, null), recepcion);

        assertEquals("OT-0001", o.codigo());
        assertEquals(EstadoOrden.RECIBIDA, o.estado());
        assertEquals("Ruido al frenar", o.falla());
        assertEquals(41000, auto.getKilometraje());
        assertEquals(20L, o.mecanicoId());
        verify(historiales).save(argThat(h -> h.getEstadoNuevo() == EstadoOrden.RECIBIDA && h.getEstadoAnterior() == null));
    }

    @Test
    @DisplayName("Un vehículo con una orden abierta no puede tener otra")
    void unaOrdenAbiertaPorVehiculo() {
        when(ordenes.existsByVehiculoIdAndEstadoIn(eq(5L), any())).thenReturn(true);
        assertThrows(ConflictoException.class,
                () -> service.crear(new OrdenCrearRequest(5L, "Falla", null, null, null, null), recepcion));
    }

    @Test
    @DisplayName("El kilometraje no puede bajar y el mecánico asignado debe tener rol mecánico")
    void validacionesAlCrear() {
        assertThrows(ValidacionException.class,
                () -> service.crear(new OrdenCrearRequest(5L, "Falla", null, 39000, null, null), recepcion));
        when(usuarios.findById(2L)).thenReturn(Optional.of(usuario(2L, Rol.RECEPCION)));
        assertThrows(ValidacionException.class,
                () -> service.crear(new OrdenCrearRequest(5L, "Falla", 2L, null, null, null), recepcion));
    }

    @Test
    @DisplayName("Crear desde una cita la marca como completada")
    void crearDesdeCita() {
        Servicio s = new Servicio();
        s.setNombre("Frenos");
        Cita c = new Cita();
        c.setId(4L);
        c.setCodigo("CITA-0004");
        c.setServicio(s);
        c.setMotivo("chilla al frenar");
        c.setCliente(auto.getCliente());
        c.setVehiculo(auto);
        when(citas.findById(4L)).thenReturn(Optional.of(c));

        OrdenDetalleDto o = service.crearDesdeCita(4L, recepcion);

        assertEquals("Frenos: chilla al frenar", o.falla());
        assertEquals(4L, o.citaId());
        assertEquals(EstadoCita.COMPLETADA, c.getEstado());
    }

    // ------------------------------------------------------------------ Permisos

    @Test
    @DisplayName("Un mecánico no puede ver ni editar órdenes que no son suyas")
    void mecanicoSoloSusOrdenes() {
        orden(EstadoOrden.EN_DIAGNOSTICO);
        assertThrows(AccesoDenegadoNegocioException.class, () -> service.obtener(9L, otroMecanico));
        assertDoesNotThrow(() -> service.obtener(9L, mecanicoActor));
    }

    @Test
    @DisplayName("El mecánico solo registra diagnóstico y trabajos; no cambia precios")
    void mecanicoNoCambiaPrecios() {
        OrdenTrabajo o = orden(EstadoOrden.EN_DIAGNOSTICO);
        OrdenActualizarRequest req = new OrdenActualizarRequest("Otra falla", " Pastillas gastadas ", null,
                new BigDecimal("50"), new BigDecimal("999"), null, null);

        service.actualizar(9L, req, mecanicoActor);

        assertEquals("Pastillas gastadas", o.getDiagnostico());
        assertEquals("Ruido al frenar", o.getFalla());
        assertEquals(0, o.getManoObra().signum());
    }

    @Test
    @DisplayName("Recepción no puede pasar una orden a diagnóstico (eso le toca al mecánico)")
    void rolNoPermitido() {
        orden(EstadoOrden.RECIBIDA);
        assertThrows(AccesoDenegadoNegocioException.class, () -> pasarA(EstadoOrden.EN_DIAGNOSTICO, recepcion));
    }

    // ------------------------------------------------------------------ Detalle

    @Test
    @DisplayName("Agregar un servicio 'según diagnóstico' exige precio; recepción puede indicarlo y el mecánico no")
    void servicioSegunDiagnostico() {
        OrdenTrabajo o = orden(EstadoOrden.EN_DIAGNOSTICO);
        Servicio diagnostico = new Servicio();
        diagnostico.setId(11L);
        diagnostico.setNombre("Diagnóstico de motor");
        when(servicios.findById(11L)).thenReturn(Optional.of(diagnostico));

        assertThrows(ValidacionException.class, () -> service.agregarServicio(9L,
                new AgregarServicioRequest(11L, 1, new BigDecimal("120"), null), mecanicoActor));

        OrdenDetalleDto d = service.agregarServicio(9L, new AgregarServicioRequest(11L, 1, new BigDecimal("120"), null), recepcion);
        assertEquals(new BigDecimal("120.00"), d.total());
        assertEquals("Diagnóstico de motor", o.getServicios().get(0).getDescripcion());
    }

    @Test
    @DisplayName("Agregar el mismo repuesto suma cantidades sin pasar el stock disponible")
    void repuestoRespetaStock() {
        OrdenTrabajo o = orden(EstadoOrden.EN_REPARACION);

        service.agregarRepuesto(9L, new AgregarRepuestoRequest(30L, 3), recepcion);
        service.agregarRepuesto(9L, new AgregarRepuestoRequest(30L, 2), recepcion);
        assertEquals(1, o.getRepuestos().size());
        assertEquals(5, o.getRepuestos().get(0).getCantidad());
        assertEquals(new BigDecimal("140.00"), o.getTotal());

        assertThrows(NegocioException.class, () -> service.agregarRepuesto(9L, new AgregarRepuestoRequest(30L, 1), recepcion));
    }

    @Test
    @DisplayName("Una orden completada ya no admite cambios en el detalle")
    void completadaNoSeEdita() {
        orden(EstadoOrden.COMPLETADA);
        assertThrows(NegocioException.class, () -> service.agregarRepuesto(9L, new AgregarRepuestoRequest(30L, 1), recepcion));
    }

    // ------------------------------------------------------------------ Estados

    @Test
    @DisplayName("No se salta estados: de RECIBIDA no se pasa directo a COMPLETADA")
    void noSeSaltanEstados() {
        orden(EstadoOrden.RECIBIDA);
        assertThrows(NegocioException.class, () -> pasarA(EstadoOrden.COMPLETADA, admin));
    }

    @Test
    @DisplayName("Cada paso exige su requisito: mecánico, diagnóstico y trabajos realizados")
    void requisitosDeCadaPaso() {
        OrdenTrabajo o = orden(EstadoOrden.RECIBIDA);
        o.setMecanico(null);
        assertThrows(NegocioException.class, () -> pasarA(EstadoOrden.EN_DIAGNOSTICO, admin));
        o.setMecanico(mecanico);
        assertEquals(EstadoOrden.EN_DIAGNOSTICO, pasarA(EstadoOrden.EN_DIAGNOSTICO, mecanicoActor).estado());

        assertThrows(NegocioException.class, () -> pasarA(EstadoOrden.EN_REPARACION, mecanicoActor));
        o.setDiagnostico("Pastillas gastadas");
        assertEquals(EstadoOrden.EN_REPARACION, pasarA(EstadoOrden.EN_REPARACION, mecanicoActor).estado());

        assertThrows(NegocioException.class, () -> pasarA(EstadoOrden.COMPLETADA, mecanicoActor));
        verify(historiales, times(2)).save(any());
    }

    @Test
    @DisplayName("Completar descuenta el inventario una sola vez y registra la fecha de cierre")
    void completarDescuentaInventario() {
        OrdenTrabajo o = orden(EstadoOrden.EN_REPARACION);
        o.setTrabajosRealizados("Cambio de filtro");
        DetalleOrdenRepuesto linea = lineaFiltro(o, 2);
        when(repuestos.findAllByIdForUpdate(List.of(30L))).thenReturn(List.of(filtro));

        OrdenDetalleDto d = pasarA(EstadoOrden.COMPLETADA, mecanicoActor);

        assertEquals(EstadoOrden.COMPLETADA, d.estado());
        assertNotNull(o.getFechaCierre());
        assertTrue(linea.isDescontado());
        verify(inventario).salidaPorOrden(eq(filtro), eq(2), eq(9L), eq("OT9-L70"), any(), anyString());
    }

    @Test
    @DisplayName("Sin comprobante emitido no se entrega el vehículo")
    void entregarExigeComprobante() {
        orden(EstadoOrden.COMPLETADA);
        assertThrows(NegocioException.class, () -> pasarA(EstadoOrden.ENTREGADA, recepcion));

        when(comprobantes.existsByOrdenIdAndEstado(9L, EstadoComprobante.EMITIDO)).thenReturn(true);
        assertEquals(EstadoOrden.ENTREGADA, pasarA(EstadoOrden.ENTREGADA, recepcion).estado());
    }

    @Test
    @DisplayName("Cancelar una orden completada exige motivo y devuelve los repuestos al inventario")
    void cancelarDevuelveInventario() {
        OrdenTrabajo o = orden(EstadoOrden.COMPLETADA);
        DetalleOrdenRepuesto linea = lineaFiltro(o, 2);
        linea.setDescontado(true);
        when(repuestos.findAllByIdForUpdate(List.of(30L))).thenReturn(List.of(filtro));

        assertThrows(ValidacionException.class, () -> pasarA(EstadoOrden.CANCELADA, admin));
        assertThrows(AccesoDenegadoNegocioException.class,
                () -> service.cambiarEstado(9L, new CambioEstadoRequest(EstadoOrden.CANCELADA, "El cliente desistió"), recepcion));

        OrdenDetalleDto d = service.cambiarEstado(9L, new CambioEstadoRequest(EstadoOrden.CANCELADA, "El cliente desistió"), admin);

        assertEquals(EstadoOrden.CANCELADA, d.estado());
        assertFalse(linea.isDescontado());
        verify(inventario).devolucionPorOrden(eq(filtro), eq(2), eq(9L), eq("OT9-L70-REV"), any(), anyString());
    }

    @Test
    @DisplayName("Con comprobante emitido no se cancela la orden")
    void noCancelaConComprobante() {
        orden(EstadoOrden.COMPLETADA);
        when(comprobantes.existsByOrdenIdAndEstado(9L, EstadoComprobante.EMITIDO)).thenReturn(true);
        assertThrows(NegocioException.class,
                () -> service.cambiarEstado(9L, new CambioEstadoRequest(EstadoOrden.CANCELADA, "motivo"), admin));
    }

    @Test
    @DisplayName("Los estados permitidos que ve cada rol dependen de la política")
    void estadosPermitidosPorRol() {
        orden(EstadoOrden.COMPLETADA);
        assertEquals(List.of(EstadoOrden.ENTREGADA), service.obtener(9L, recepcion).transicionesPermitidas());
        assertEquals(List.of(EstadoOrden.ENTREGADA, EstadoOrden.CANCELADA), service.obtener(9L, admin).transicionesPermitidas());
        assertTrue(service.obtener(9L, mecanicoActor).transicionesPermitidas().isEmpty());
    }
}
