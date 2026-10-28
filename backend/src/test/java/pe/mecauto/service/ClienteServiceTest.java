package pe.mecauto.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.mecauto.dao.ClienteDAO;
import pe.mecauto.domain.TipoDocumento;
import pe.mecauto.dto.PersonaDtos.ClienteDto;
import pe.mecauto.dto.PersonaDtos.ClienteRequest;
import pe.mecauto.entity.Cliente;
import pe.mecauto.exception.ConflictoException;
import pe.mecauto.exception.NegocioException;
import pe.mecauto.exception.RecursoNoEncontradoException;
import pe.mecauto.exception.ValidacionException;
import pe.mecauto.repository.ClienteRepository;
import pe.mecauto.repository.OrdenTrabajoRepository;
import pe.mecauto.repository.VehiculoRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Reglas de clientes con repositorios simulados (no requiere SQL Server). */
@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock ClienteRepository repo;
    @Mock VehiculoRepository vehiculos;
    @Mock OrdenTrabajoRepository ordenes;
    @Mock ClienteDAO dao;

    ClienteService service;

    @BeforeEach
    void preparar() {
        service = new ClienteService(repo, vehiculos, ordenes, dao);
    }

    private static ClienteRequest dni(String numero) {
        return new ClienteRequest(TipoDocumento.DNI, numero, "  Juan   Pérez  ", " 987654321 ", "", null);
    }

    private static Cliente cliente(long id, String doc) {
        Cliente c = new Cliente();
        c.setId(id);
        c.setTipoDocumento(TipoDocumento.DNI);
        c.setNroDocumento(doc);
        c.setNombre("Juan Pérez");
        return c;
    }

    @Test
    @DisplayName("Crear normaliza nombre, teléfono y deja vacíos como null")
    void crearNormalizaDatos() {
        when(dao.existePorDocumento("12345678")).thenReturn(false);
        when(repo.save(any(Cliente.class))).thenAnswer(i -> i.getArgument(0));

        ClienteDto c = service.crear(dni(" 12345678 "));

        assertEquals("12345678", c.nroDocumento());
        assertEquals("Juan Pérez", c.nombre());
        assertEquals("987654321", c.telefono());
        assertNull(c.correo());
        assertTrue(c.activo());
    }

    @Test
    @DisplayName("No se puede crear un cliente con un documento repetido")
    void documentoRepetido() {
        when(dao.existePorDocumento("12345678")).thenReturn(true);
        assertThrows(ConflictoException.class, () -> service.crear(dni("12345678")));
        verify(repo, never()).save(any());
    }

    @Test
    @DisplayName("Un DNI con formato incorrecto se rechaza indicando el campo")
    void dniInvalido() {
        ValidacionException e = assertThrows(ValidacionException.class, () -> service.crear(dni("1234")));
        assertTrue(e.getCampos().containsKey("nroDocumento"));
        verifyNoInteractions(dao);
    }

    @Test
    @DisplayName("Actualizar con el documento de otro cliente da conflicto")
    void actualizarConDocumentoDeOtro() {
        when(repo.findById(1L)).thenReturn(Optional.of(cliente(1L, "12345678")));
        when(dao.existeOtroConDocumento("87654321", 1L)).thenReturn(true);
        assertThrows(ConflictoException.class, () -> service.actualizar(1L, dni("87654321")));
    }

    @Test
    @DisplayName("Actualizar cambia los datos del mismo cliente")
    void actualizarCambiaDatos() {
        Cliente c = cliente(1L, "12345678");
        when(repo.findById(1L)).thenReturn(Optional.of(c));
        when(dao.existeOtroConDocumento("87654321", 1L)).thenReturn(false);

        service.actualizar(1L, dni("87654321"));

        assertEquals("87654321", c.getNroDocumento());
    }

    @Test
    @DisplayName("No se desactiva un cliente con órdenes abiertas")
    void noDesactivaConOrdenesAbiertas() {
        Cliente c = cliente(1L, "12345678");
        when(repo.findById(1L)).thenReturn(Optional.of(c));
        when(ordenes.existsByClienteIdAndEstadoIn(eq(1L), any())).thenReturn(true);

        assertThrows(NegocioException.class, () -> service.cambiarEstado(1L, false));
        assertTrue(c.isActivo());
    }

    @Test
    @DisplayName("Sin órdenes abiertas el cliente se desactiva y se puede reactivar")
    void desactivaYReactiva() {
        Cliente c = cliente(1L, "12345678");
        when(repo.findById(1L)).thenReturn(Optional.of(c));
        when(ordenes.existsByClienteIdAndEstadoIn(eq(1L), any())).thenReturn(false);

        assertFalse(service.cambiarEstado(1L, false).activo());
        assertTrue(service.cambiarEstado(1L, true).activo());
    }

    @Test
    @DisplayName("Un cliente inexistente responde 'no encontrado'")
    void inexistente() {
        when(repo.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RecursoNoEncontradoException.class, () -> service.obtener(99L));
        assertThrows(RecursoNoEncontradoException.class, () -> service.vehiculosDe(99L));
    }
}
