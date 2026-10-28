package pe.mecauto.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.mecauto.domain.*;
import pe.mecauto.dto.ComprobanteDtos.AnularComprobanteRequest;
import pe.mecauto.dto.ComprobanteDtos.ComprobanteDto;
import pe.mecauto.dto.ComprobanteDtos.EmitirComprobanteRequest;
import pe.mecauto.entity.*;
import pe.mecauto.exception.ConflictoException;
import pe.mecauto.exception.NegocioException;
import pe.mecauto.repository.ComprobanteRepository;
import pe.mecauto.repository.OrdenTrabajoRepository;
import pe.mecauto.repository.UsuarioRepository;
import pe.mecauto.security.UsuarioPrincipal;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Comprobantes internos (RN05) con repositorios simulados y la fábrica real. */
@ExtendWith(MockitoExtension.class)
class ComprobanteServiceTest {

    @Mock ComprobanteRepository comprobantes;
    @Mock OrdenTrabajoRepository ordenes;
    @Mock UsuarioRepository usuarios;
    @Mock SecuenciaService secuencias;
    @Mock ConfiguracionService config;

    ComprobanteService service;
    OrdenTrabajo orden;
    Cliente cliente;
    UsuarioPrincipal cajero;

    @BeforeEach
    void preparar() {
        service = new ComprobanteService(comprobantes, ordenes, usuarios, secuencias, config, new ComprobanteFactory());
        cliente = new Cliente();
        cliente.setId(1L);
        cliente.setNombre("Juan Pérez");
        cliente.setTipoDocumento(TipoDocumento.DNI);
        cliente.setNroDocumento("12345678");
        Vehiculo v = new Vehiculo();
        v.setPlaca("ABC-123");

        orden = new OrdenTrabajo();
        orden.setId(9L);
        orden.setCodigo("OT-0009");
        orden.setCliente(cliente);
        orden.setVehiculo(v);
        orden.setEstado(EstadoOrden.COMPLETADA);
        DetalleOrdenServicio s = new DetalleOrdenServicio();
        s.setDescripcion("Cambio de aceite");
        s.setCantidad(1);
        s.setPrecioUnitario(new BigDecimal("80.00"));
        orden.getServicios().add(s);
        Repuesto filtro = new Repuesto();
        filtro.setCodigo("FIL-01");
        filtro.setNombre("Filtro de aceite");
        DetalleOrdenRepuesto r = new DetalleOrdenRepuesto();
        r.setRepuesto(filtro);
        r.setCantidad(2);
        r.setPrecioUnitario(new BigDecimal("28.00"));
        orden.getRepuestos().add(r);
        orden.setManoObra(new BigDecimal("40.00"));
        orden.setDescuento(new BigDecimal("12.00"));
        orden.recalcularTotal();
        lenient().when(ordenes.findByIdForUpdate(9L)).thenReturn(Optional.of(orden));

        Usuario u = new Usuario();
        u.setId(2L);
        u.setNombre("Ángel Flores");
        u.setRol(Rol.RECEPCION);
        cajero = new UsuarioPrincipal(u);
        lenient().when(usuarios.getReferenceById(2L)).thenReturn(u);
        lenient().when(config.obtener("serie_boleta")).thenReturn("B001");
        lenient().when(config.obtener("serie_factura")).thenReturn("F001");
        lenient().when(config.datosEmisor()).thenReturn(Map.of("nombre", "Taller MecAuto"));
        lenient().when(secuencias.siguiente(any())).thenReturn(15);
    }

    private ComprobanteDto emitir() {
        return service.emitir(new EmitirComprobanteRequest(9L, MetodoPago.YAPE_PLIN), cajero);
    }

    @Test
    @DisplayName("Cliente con DNI recibe boleta con número correlativo y el IGV desglosado del total")
    void boletaParaDni() {
        ComprobanteDto c = emitir();

        assertEquals(TipoComprobante.BOLETA, c.tipo());
        assertEquals("B001-00000015", c.numeroCompleto());
        assertEquals(new BigDecimal("164.00"), c.total());
        assertEquals(new BigDecimal("138.98"), c.baseImponible());
        assertEquals(new BigDecimal("25.02"), c.igv());
        assertEquals(c.total(), c.baseImponible().add(c.igv()));
        assertEquals("Ángel Flores", c.emitidoPor());
        assertEquals("Taller MecAuto", c.taller().get("nombre"));
        verify(comprobantes).save(any(Comprobante.class));
    }

    @Test
    @DisplayName("Las líneas incluyen servicios, repuestos con código, mano de obra y el descuento en negativo")
    void lineasDelComprobante() {
        ComprobanteDto c = emitir();

        assertEquals(4, c.detalles().size());
        assertEquals("Filtro de aceite (FIL-01)", c.detalles().get(1).descripcion());
        assertEquals(new BigDecimal("56.00"), c.detalles().get(1).importe());
        assertEquals("Mano de obra", c.detalles().get(2).descripcion());
        assertEquals("Descuento", c.detalles().get(3).descripcion());
        assertEquals(-1, c.detalles().get(3).importe().signum());
    }

    @Test
    @DisplayName("Cliente con RUC recibe factura de la serie de facturas")
    void facturaParaRuc() {
        cliente.setTipoDocumento(TipoDocumento.RUC);
        cliente.setNroDocumento("20123456789");

        ComprobanteDto c = emitir();

        assertEquals(TipoComprobante.FACTURA, c.tipo());
        assertEquals("F001", c.serie());
        verify(secuencias).siguiente("F001");
    }

    @Test
    @DisplayName("Solo se emite para una orden COMPLETADA")
    void soloOrdenCompletada() {
        orden.setEstado(EstadoOrden.EN_REPARACION);
        assertThrows(NegocioException.class, this::emitir);
        verify(comprobantes, never()).save(any());
    }

    @Test
    @DisplayName("Una orden no puede tener dos comprobantes emitidos")
    void unoPorOrden() {
        when(comprobantes.existsByOrdenIdAndEstado(9L, EstadoComprobante.EMITIDO)).thenReturn(true);
        assertThrows(ConflictoException.class, this::emitir);
    }

    @Test
    @DisplayName("Una orden con total cero no genera comprobante")
    void totalCero() {
        orden.getServicios().clear();
        orden.getRepuestos().clear();
        orden.setManoObra(BigDecimal.ZERO);
        orden.recalcularTotal();
        assertThrows(NegocioException.class, this::emitir);
    }

    @Test
    @DisplayName("Anular guarda el motivo; no se anula dos veces ni si la orden ya se entregó")
    void anular() {
        Comprobante c = new ComprobanteFactory().crear(orden, MetodoPago.EFECTIVO, null, "B001", "00000001");
        c.setId(3L);
        when(comprobantes.findById(3L)).thenReturn(Optional.of(c));

        ComprobanteDto anulado = service.anular(3L, new AnularComprobanteRequest("  Error en el método de pago "));
        assertEquals(EstadoComprobante.ANULADO, anulado.estado());
        assertEquals("Error en el método de pago", anulado.motivoAnulacion());
        assertThrows(NegocioException.class, () -> service.anular(3L, new AnularComprobanteRequest("otra vez")));

        c.setEstado(EstadoComprobante.EMITIDO);
        orden.setEstado(EstadoOrden.ENTREGADA);
        assertThrows(NegocioException.class, () -> service.anular(3L, new AnularComprobanteRequest("tarde")));
    }
}
