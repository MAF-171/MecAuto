package pe.mecauto.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.mecauto.domain.EstadoOrden;
import pe.mecauto.dto.PersonaDtos.HistorialMantenimientoDto;
import pe.mecauto.dto.PersonaDtos.VehiculoDto;
import pe.mecauto.dto.PersonaDtos.VehiculoRequest;
import pe.mecauto.entity.Cliente;
import pe.mecauto.entity.OrdenTrabajo;
import pe.mecauto.entity.Usuario;
import pe.mecauto.entity.Vehiculo;
import pe.mecauto.exception.ConflictoException;
import pe.mecauto.exception.NegocioException;
import pe.mecauto.exception.RecursoNoEncontradoException;
import pe.mecauto.exception.ValidacionException;
import pe.mecauto.repository.ClienteRepository;
import pe.mecauto.repository.OrdenTrabajoRepository;
import pe.mecauto.repository.VehiculoRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Reglas de vehículos con repositorios simulados (no requiere SQL Server). */
@ExtendWith(MockitoExtension.class)
class VehiculoServiceTest {

    @Mock VehiculoRepository repo;
    @Mock ClienteRepository clientes;
    @Mock OrdenTrabajoRepository ordenes;

    VehiculoService service;
    Cliente juan;

    @BeforeEach
    void preparar() {
        service = new VehiculoService(repo, clientes, ordenes);
        juan = new Cliente();
        juan.setId(1L);
        juan.setNombre("Juan Pérez");
    }

    private static VehiculoRequest req(long clienteId, String placa) {
        return new VehiculoRequest(clienteId, placa, " Toyota ", " Corolla ", 2018, "", 45000);
    }

    private Vehiculo vehiculo(long id, String placa) {
        Vehiculo v = new Vehiculo();
        v.setId(id);
        v.setCliente(juan);
        v.setPlaca(placa);
        v.setMarca("Toyota");
        v.setModelo("Corolla");
        return v;
    }

    @Test
    @DisplayName("Crear normaliza la placa y limpia los textos")
    void crearNormaliza() {
        when(repo.existsByPlaca("ABC-123")).thenReturn(false);
        when(clientes.findById(1L)).thenReturn(Optional.of(juan));
        when(repo.save(any(Vehiculo.class))).thenAnswer(i -> i.getArgument(0));

        VehiculoDto v = service.crear(req(1L, " abc-123 "));

        assertEquals("ABC-123", v.placa());
        assertEquals("Toyota", v.marca());
        assertEquals("Corolla", v.modelo());
        assertNull(v.color());
        assertEquals("Juan Pérez", v.clienteNombre());
    }

    @Test
    @DisplayName("Una placa con formato inválido se rechaza")
    void placaInvalida() {
        assertThrows(ValidacionException.class, () -> service.crear(req(1L, "A1")));
        verify(repo, never()).save(any());
    }

    @Test
    @DisplayName("No se repiten placas")
    void placaRepetida() {
        when(repo.existsByPlaca("ABC-123")).thenReturn(true);
        assertThrows(ConflictoException.class, () -> service.crear(req(1L, "ABC-123")));
    }

    @Test
    @DisplayName("No se registra un vehículo para un cliente desactivado o inexistente")
    void clienteNoValido() {
        juan.setActivo(false);
        when(clientes.findById(1L)).thenReturn(Optional.of(juan));
        when(clientes.findById(9L)).thenReturn(Optional.empty());

        assertThrows(NegocioException.class, () -> service.crear(req(1L, "ABC-123")));
        assertThrows(RecursoNoEncontradoException.class, () -> service.crear(req(9L, "XYZ-999")));
    }

    @Test
    @DisplayName("Actualizar con la placa de otro vehículo da conflicto; con la propia no")
    void actualizarPlaca() {
        Vehiculo propio = vehiculo(1L, "ABC-123");
        when(repo.findById(1L)).thenReturn(Optional.of(propio));
        when(repo.findByPlaca("XYZ-999")).thenReturn(Optional.of(vehiculo(2L, "XYZ-999")));
        when(repo.findByPlaca("ABC-123")).thenReturn(Optional.of(propio));

        assertThrows(ConflictoException.class, () -> service.actualizar(1L, req(1L, "XYZ-999")));
        assertDoesNotThrow(() -> service.actualizar(1L, req(1L, "ABC-123")));
    }

    @Test
    @DisplayName("No se desactiva un vehículo con órdenes abiertas")
    void noDesactivaConOrdenes() {
        Vehiculo v = vehiculo(1L, "ABC-123");
        when(repo.findById(1L)).thenReturn(Optional.of(v));
        when(ordenes.existsByVehiculoIdAndEstadoIn(eq(1L), any())).thenReturn(true);

        assertThrows(NegocioException.class, () -> service.cambiarEstado(1L, false));
        assertTrue(v.isActivo());
    }

    @Test
    @DisplayName("El historial se arma con las órdenes del vehículo")
    void historialDesdeOrdenes() {
        when(repo.findById(1L)).thenReturn(Optional.of(vehiculo(1L, "ABC-123")));
        Usuario mecanico = new Usuario();
        mecanico.setNombre("David Hans");
        OrdenTrabajo o = new OrdenTrabajo();
        o.setId(5L);
        o.setCodigo("OT-0005");
        o.setEstado(EstadoOrden.ENTREGADA);
        o.setMecanico(mecanico);
        OrdenTrabajo sinMecanico = new OrdenTrabajo();
        sinMecanico.setId(6L);
        sinMecanico.setCodigo("OT-0006");
        when(ordenes.findByVehiculoIdOrderByFechaIngresoDesc(1L)).thenReturn(List.of(o, sinMecanico));

        List<HistorialMantenimientoDto> h = service.historial(1L);

        assertEquals(2, h.size());
        assertEquals("OT-0005", h.get(0).codigo());
        assertEquals("David Hans", h.get(0).mecanico());
        assertNull(h.get(1).mecanico());
    }
}
