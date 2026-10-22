package pe.mecauto.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import pe.mecauto.domain.Rol;
import pe.mecauto.domain.TipoMovimiento;
import pe.mecauto.dto.CatalogoDtos.MovimientoRequest;
import pe.mecauto.entity.MovimientoInventario;
import pe.mecauto.entity.Repuesto;
import pe.mecauto.entity.Usuario;
import pe.mecauto.exception.AccesoDenegadoNegocioException;
import pe.mecauto.exception.NegocioException;
import pe.mecauto.exception.ValidacionException;
import pe.mecauto.repository.MovimientoInventarioRepository;
import pe.mecauto.repository.ProveedorRepository;
import pe.mecauto.repository.RepuestoRepository;
import pe.mecauto.repository.UsuarioRepository;
import pe.mecauto.security.UsuarioPrincipal;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Reglas de inventario con repositorios simulados (no requiere SQL Server). */
@ExtendWith(MockitoExtension.class)
class InventarioServiceTest {

    @Mock RepuestoRepository repuestos;
    @Mock MovimientoInventarioRepository movimientos;
    @Mock ProveedorRepository proveedores;
    @Mock UsuarioRepository usuarios;
    @Mock ApplicationEventPublisher eventos;

    InventarioService service;
    Repuesto filtro;

    @BeforeEach
    void preparar() {
        service = new InventarioService(repuestos, movimientos, proveedores, usuarios, eventos);
        filtro = new Repuesto();
        filtro.setId(1L);
        filtro.setCodigo("FA-102");
        filtro.setNombre("Filtro de aceite");
        filtro.setPrecioVenta(new BigDecimal("28.00"));
        filtro.setStockActual(5);
        filtro.setStockMinimo(2);
        filtro.setActivo(true);
    }

    private UsuarioPrincipal actor(Rol rol) {
        Usuario u = new Usuario();
        u.setId(10L);
        u.setUsername("u");
        u.setPasswordHash("x");
        u.setNombre("Usuario");
        u.setRol(rol);
        u.setActivo(true);
        return new UsuarioPrincipal(u);
    }

    @Test
    @DisplayName("Una salida mayor al stock es rechazada y no cambia el stock (RN03)")
    void salidaMayorAlStock() {
        when(repuestos.findByIdForUpdate(1L)).thenReturn(Optional.of(filtro));
        var req = new MovimientoRequest(TipoMovimiento.SALIDA, 6, "Uso", null);

        assertThrows(NegocioException.class, () -> service.registrar(1L, req, actor(Rol.ADMINISTRADOR)));

        assertEquals(5, filtro.getStockActual());
        verify(movimientos, never()).save(any());
    }

    @Test
    @DisplayName("Una salida válida descuenta stock y guarda el stock resultante")
    void salidaValida() {
        when(repuestos.findByIdForUpdate(1L)).thenReturn(Optional.of(filtro));
        var req = new MovimientoRequest(TipoMovimiento.SALIDA, 3, "Uso", null);

        service.registrar(1L, req, actor(Rol.ADMINISTRADOR));

        assertEquals(2, filtro.getStockActual());
        ArgumentCaptor<MovimientoInventario> cap = ArgumentCaptor.forClass(MovimientoInventario.class);
        verify(movimientos).save(cap.capture());
        assertEquals(-3, cap.getValue().getCantidad());
        assertEquals(2, cap.getValue().getStockResultante());
    }

    @Test
    @DisplayName("Al quedar en el mínimo se publica la alerta de stock bajo")
    void alertaStockBajo() {
        when(repuestos.findByIdForUpdate(1L)).thenReturn(Optional.of(filtro));
        service.registrar(1L, new MovimientoRequest(TipoMovimiento.SALIDA, 3, null, null), actor(Rol.ADMINISTRADOR));
        verify(eventos).publishEvent(any(StockBajoEvent.class));
    }

    @Test
    @DisplayName("Una entrada suma stock")
    void entrada() {
        when(repuestos.findByIdForUpdate(1L)).thenReturn(Optional.of(filtro));
        service.registrar(1L, new MovimientoRequest(TipoMovimiento.ENTRADA, 10, "Compra", null), actor(Rol.RECEPCION));
        assertEquals(15, filtro.getStockActual());
        verify(eventos, never()).publishEvent(any());
    }

    @Test
    @DisplayName("El ajuste exige motivo y fija el stock contado")
    void ajuste() {
        when(repuestos.findByIdForUpdate(1L)).thenReturn(Optional.of(filtro));
        assertThrows(ValidacionException.class,
                () -> service.registrar(1L, new MovimientoRequest(TipoMovimiento.AJUSTE, 8, "  ", null), actor(Rol.ADMINISTRADOR)));

        service.registrar(1L, new MovimientoRequest(TipoMovimiento.AJUSTE, 8, "Conteo físico", null), actor(Rol.ADMINISTRADOR));
        assertEquals(8, filtro.getStockActual());
    }

    @Test
    @DisplayName("El mecánico solo puede registrar salidas; recepción no ajusta")
    void permisosPorRol() {
        assertThrows(AccesoDenegadoNegocioException.class,
                () -> service.registrar(1L, new MovimientoRequest(TipoMovimiento.ENTRADA, 1, null, null), actor(Rol.MECANICO)));
        assertThrows(AccesoDenegadoNegocioException.class,
                () -> service.registrar(1L, new MovimientoRequest(TipoMovimiento.AJUSTE, 1, "x", null), actor(Rol.RECEPCION)));
    }

    @Test
    @DisplayName("Misma clave de idempotencia: la operación no se repite")
    void idempotencia() {
        MovimientoInventario previo = new MovimientoInventario();
        previo.setRepuesto(filtro);
        previo.setTipo(TipoMovimiento.SALIDA);
        previo.setCantidad(-1);
        previo.setStockResultante(4);
        when(movimientos.findByClaveIdempotencia("k1")).thenReturn(Optional.of(previo));

        service.registrar(1L, new MovimientoRequest(TipoMovimiento.SALIDA, 1, null, "k1"), actor(Rol.ADMINISTRADOR));

        assertEquals(5, filtro.getStockActual());
        verify(movimientos, never()).save(any());
        verify(repuestos, never()).findByIdForUpdate(any());
    }

    @Test
    @DisplayName("Salida por orden con clave ya usada no descuenta dos veces")
    void salidaPorOrdenNoDuplica() {
        when(movimientos.existsByClaveIdempotencia("OT-1-R-7")).thenReturn(true);
        service.salidaPorOrden(filtro, 2, 1L, "OT-1-R-7", null, "Orden");
        assertEquals(5, filtro.getStockActual());
        verify(movimientos, never()).save(any());
    }

    @Test
    @DisplayName("Salida por orden descuenta una vez; la devolución repone")
    void salidaYDevolucionPorOrden() {
        when(movimientos.existsByClaveIdempotencia(any())).thenReturn(false);
        service.salidaPorOrden(filtro, 2, 1L, "OT-1-R-7", null, "Orden");
        assertEquals(3, filtro.getStockActual());
        service.devolucionPorOrden(filtro, 2, 1L, "OT-1-R-7-DEV", null, "Cancelación");
        assertEquals(5, filtro.getStockActual());
    }

    @Test
    @DisplayName("Salida por orden con cantidad 0 se rechaza (precondición de Guava) y no toca el stock")
    void salidaPorOrdenExigeCantidadPositiva() {
        assertThrows(IllegalArgumentException.class,
                () -> service.salidaPorOrden(filtro, 0, 1L, "OT-1-R-8", null, "Orden"));
        assertEquals(5, filtro.getStockActual());
        verify(movimientos, never()).save(any());
    }
}
