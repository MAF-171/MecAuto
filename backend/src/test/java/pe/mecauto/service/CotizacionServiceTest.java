package pe.mecauto.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.mecauto.domain.EstadoCotizacion;
import pe.mecauto.domain.Rol;
import pe.mecauto.domain.TipoLineaCotizacion;
import pe.mecauto.dto.CotizacionDtos.CotizacionDto;
import pe.mecauto.dto.CotizacionDtos.CotizacionRequest;
import pe.mecauto.dto.CotizacionDtos.LineaCotizacionRequest;
import pe.mecauto.dto.OrdenDtos.OrdenDetalleDto;
import pe.mecauto.entity.*;
import pe.mecauto.exception.NegocioException;
import pe.mecauto.exception.ValidacionException;
import pe.mecauto.repository.*;
import pe.mecauto.security.UsuarioPrincipal;
import pe.mecauto.util.Tiempo;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Cotizaciones con IGV incluido (RN06) y su ciclo de estados, con repositorios simulados. */
@ExtendWith(MockitoExtension.class)
class CotizacionServiceTest {

    @Mock CotizacionRepository cotizaciones;
    @Mock ClienteRepository clientes;
    @Mock VehiculoRepository vehiculos;
    @Mock ServicioRepository servicios;
    @Mock RepuestoRepository repuestos;
    @Mock OrdenTrabajoRepository ordenes;
    @Mock UsuarioRepository usuarios;
    @Mock SecuenciaService secuencias;
    @Mock OrdenService ordenService;

    CotizacionService service;
    Cliente juan;
    UsuarioPrincipal recepcion;

    @BeforeEach
    void preparar() {
        service = new CotizacionService(cotizaciones, clientes, vehiculos, servicios, repuestos, ordenes, usuarios,
                secuencias, ordenService);
        juan = new Cliente();
        juan.setId(1L);
        juan.setNombre("Juan Pérez");
        lenient().when(clientes.findById(1L)).thenReturn(Optional.of(juan));
        Usuario u = new Usuario();
        u.setId(3L);
        u.setUsername("recepcion");
        u.setRol(Rol.RECEPCION);
        recepcion = new UsuarioPrincipal(u);
        lenient().when(usuarios.getReferenceById(3L)).thenReturn(u);
        lenient().when(secuencias.siguienteCodigo("COT")).thenReturn("COT-0001");
        lenient().when(cotizaciones.save(any(Cotizacion.class))).thenAnswer(i -> i.getArgument(0));

        Servicio aceite = new Servicio();
        aceite.setId(10L);
        aceite.setNombre("Cambio de aceite");
        aceite.setPrecioReferencial(new BigDecimal("80.00"));
        lenient().when(servicios.findById(10L)).thenReturn(Optional.of(aceite));
        Repuesto filtro = new Repuesto();
        filtro.setId(20L);
        filtro.setNombre("Filtro de aceite");
        filtro.setPrecioVenta(new BigDecimal("28.00"));
        lenient().when(repuestos.findById(20L)).thenReturn(Optional.of(filtro));
    }

    private static CotizacionRequest req(BigDecimal descuento, LineaCotizacionRequest... lineas) {
        return new CotizacionRequest(1L, null, null, descuento, " ", List.of(lineas));
    }

    private static LineaCotizacionRequest servicio(int cantidad) {
        return new LineaCotizacionRequest(TipoLineaCotizacion.SERVICIO, 10L, null, null, cantidad, null);
    }

    private static LineaCotizacionRequest repuesto(int cantidad) {
        return new LineaCotizacionRequest(TipoLineaCotizacion.REPUESTO, null, 20L, null, cantidad, null);
    }

    private Cotizacion guardada(EstadoCotizacion estado) {
        Cotizacion c = new Cotizacion();
        c.setId(5L);
        c.setCodigo("COT-0005");
        c.setCliente(juan);
        c.setEstado(estado);
        c.setVigenciaHasta(Tiempo.hoy().plusDays(5));
        when(cotizaciones.findByIdForUpdate(5L)).thenReturn(Optional.of(c));
        return c;
    }

    @Test
    @DisplayName("Toma precio y nombre del catálogo y desglosa el IGV del total (no lo suma)")
    void calculaTotalesConIgvIncluido() {
        CotizacionDto c = service.crear(req(new BigDecimal("10"), servicio(2), repuesto(3)), recepcion);

        assertEquals("COT-0001", c.codigo());
        assertEquals(new BigDecimal("244.00"), c.subtotal());
        assertEquals(new BigDecimal("234.00"), c.total());
        assertEquals(new BigDecimal("198.31"), c.baseImponible());
        assertEquals(new BigDecimal("35.69"), c.igv());
        assertEquals("Cambio de aceite", c.detalles().get(0).descripcion());
        assertEquals("Filtro de aceite", c.detalles().get(1).descripcion());
        assertEquals(Tiempo.hoy().plusDays(15), c.vigenciaHasta());
        assertNull(c.observaciones());
        assertEquals(EstadoCotizacion.PENDIENTE, c.estado());
    }

    @Test
    @DisplayName("La mano de obra necesita precio y el descuento no puede superar el subtotal")
    void validacionesDeMontos() {
        LineaCotizacionRequest sinPrecio = new LineaCotizacionRequest(TipoLineaCotizacion.MANO_OBRA, null, null, null, 1, null);
        assertThrows(ValidacionException.class, () -> service.crear(req(null, sinPrecio), recepcion));
        assertThrows(ValidacionException.class, () -> service.crear(req(new BigDecimal("500"), servicio(1)), recepcion));
    }

    @Test
    @DisplayName("Una línea de servicio sin servicio elegido se rechaza")
    void lineaIncompleta() {
        LineaCotizacionRequest l = new LineaCotizacionRequest(TipoLineaCotizacion.SERVICIO, null, null, null, 1, null);
        assertThrows(ValidacionException.class, () -> service.crear(req(null, l), recepcion));
    }

    @Test
    @DisplayName("No se cotiza a un cliente desactivado ni con vigencia pasada")
    void clienteInactivoOVigenciaPasada() {
        CotizacionRequest vencida = new CotizacionRequest(1L, null, Tiempo.hoy().minusDays(1), null, null, List.of(servicio(1)));
        assertThrows(ValidacionException.class, () -> service.crear(vencida, recepcion));
        juan.setActivo(false);
        assertThrows(NegocioException.class, () -> service.crear(req(null, servicio(1)), recepcion));
    }

    @Test
    @DisplayName("Aprobar: solo pendiente y vigente")
    void aprobar() {
        Cotizacion c = guardada(EstadoCotizacion.PENDIENTE);
        c.setVigenciaHasta(Tiempo.hoy().minusDays(1));
        assertThrows(NegocioException.class, () -> service.aprobar(5L));

        c.setVigenciaHasta(Tiempo.hoy());
        assertEquals(EstadoCotizacion.APROBADA, service.aprobar(5L).estado());
        assertThrows(NegocioException.class, () -> service.aprobar(5L));
        assertThrows(NegocioException.class, () -> service.rechazar(5L));
    }

    @Test
    @DisplayName("Solo una cotización pendiente se edita")
    void editarSoloPendiente() {
        Cotizacion c = new Cotizacion();
        c.setEstado(EstadoCotizacion.RECHAZADA);
        when(cotizaciones.findById(5L)).thenReturn(Optional.of(c));
        assertThrows(NegocioException.class, () -> service.actualizar(5L, req(null, servicio(1))));
    }

    @Test
    @DisplayName("Convertir crea la orden y marca la cotización como convertida")
    void convertirEnOrden() {
        Cotizacion c = guardada(EstadoCotizacion.APROBADA);
        OrdenDetalleDto orden = mock(OrdenDetalleDto.class);
        when(orden.id()).thenReturn(40L);
        when(ordenService.crearDesdeCotizacion(c, recepcion)).thenReturn(orden);

        assertSame(orden, service.convertirEnOrden(5L, recepcion));
        assertEquals(EstadoCotizacion.CONVERTIDA, c.getEstado());
        assertEquals(40L, c.getOrdenId());
    }

    @Test
    @DisplayName("Una cotización pendiente o ya convertida no se convierte en orden")
    void noConvierteSinAprobar() {
        guardada(EstadoCotizacion.PENDIENTE);
        assertThrows(NegocioException.class, () -> service.convertirEnOrden(5L, recepcion));
        verifyNoInteractions(ordenService);
    }

    @Test
    @DisplayName("Desde una orden copia servicios, repuestos, mano de obra y descuento")
    void generarDesdeOrden() {
        OrdenTrabajo o = new OrdenTrabajo();
        o.setId(7L);
        o.setCodigo("OT-0007");
        o.setCliente(juan);
        DetalleOrdenServicio s = new DetalleOrdenServicio();
        s.setDescripcion("Cambio de aceite");
        s.setCantidad(1);
        s.setPrecioUnitario(new BigDecimal("80.00"));
        o.getServicios().add(s);
        Repuesto filtro = new Repuesto();
        filtro.setNombre("Filtro de aceite");
        DetalleOrdenRepuesto r = new DetalleOrdenRepuesto();
        r.setRepuesto(filtro);
        r.setCantidad(2);
        r.setPrecioUnitario(new BigDecimal("28.00"));
        o.getRepuestos().add(r);
        o.setManoObra(new BigDecimal("50.00"));
        o.setDescuento(new BigDecimal("6.00"));
        when(ordenes.findById(7L)).thenReturn(Optional.of(o));

        CotizacionDto c = service.generarDesdeOrden(7L, recepcion);

        assertEquals(3, c.detalles().size());
        assertEquals(TipoLineaCotizacion.MANO_OBRA, c.detalles().get(2).tipo());
        assertEquals(new BigDecimal("180.00"), c.total());
        assertEquals(7L, c.ordenId());
    }

    @Test
    @DisplayName("Una orden vacía no genera cotización")
    void ordenVacia() {
        OrdenTrabajo o = new OrdenTrabajo();
        o.setId(7L);
        o.setCliente(juan);
        when(ordenes.findById(7L)).thenReturn(Optional.of(o));
        assertThrows(NegocioException.class, () -> service.generarDesdeOrden(7L, recepcion));
    }
}
