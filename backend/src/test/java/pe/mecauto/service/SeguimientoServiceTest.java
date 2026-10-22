package pe.mecauto.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.mecauto.domain.EstadoOrden;
import pe.mecauto.dto.SeguimientoDtos.SeguimientoOrdenDto;
import pe.mecauto.entity.DetalleOrdenServicio;
import pe.mecauto.entity.OrdenTrabajo;
import pe.mecauto.entity.Vehiculo;
import pe.mecauto.exception.RecursoNoEncontradoException;
import pe.mecauto.repository.DetalleOrdenServicioRepository;
import pe.mecauto.repository.OrdenTrabajoRepository;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;

/** Consulta pública del estado de una orden, con repositorios simulados (no requiere SQL Server). */
@ExtendWith(MockitoExtension.class)
class SeguimientoServiceTest {

    @Mock OrdenTrabajoRepository ordenes;
    @Mock DetalleOrdenServicioRepository detalles;

    SeguimientoService service;
    OrdenTrabajo orden;

    @BeforeEach
    void preparar() {
        service = new SeguimientoService(ordenes, detalles);
        Vehiculo v = new Vehiculo();
        v.setPlaca("ABC-029");
        orden = new OrdenTrabajo();
        orden.setId(2L);
        orden.setCodigo("OT-0002");
        orden.setVehiculo(v);
        orden.setEstado(EstadoOrden.EN_REPARACION);
        orden.setFechaIngreso(LocalDateTime.of(2026, 10, 19, 9, 30));
        lenient().when(ordenes.findByCodigoIgnoreCase("OT-0002")).thenReturn(Optional.of(orden));
        DetalleOrdenServicio d = new DetalleOrdenServicio();
        d.setDescripcion("Cambio de aceite y filtros");
        lenient().when(detalles.findFirstByOrdenIdOrderByIdAsc(2L)).thenReturn(Optional.of(d));
    }

    @Test
    @DisplayName("Con código y placa correctos devuelve el estado y enmascara la placa")
    void devuelveEstadoConPlacaEnmascarada() {
        SeguimientoOrdenDto r = service.consultar(" ot-0002 ", "029");
        assertEquals("OT-0002", r.codigo());
        assertEquals(EstadoOrden.EN_REPARACION, r.estado());
        assertEquals("***-029", r.placa());
        assertEquals("Cambio de aceite y filtros", r.servicio());
    }

    @Test
    @DisplayName("Acepta la placa completa con guion y en minúsculas")
    void aceptaPlacaCompleta() {
        assertEquals("***-029", service.consultar("OT-0002", "abc-029").placa());
    }

    @Test
    @DisplayName("Con placa equivocada responde lo mismo que si el código no existiera")
    void placaEquivocadaNoRevelaNada() {
        RecursoNoEncontradoException ex = assertThrows(RecursoNoEncontradoException.class, () -> service.consultar("OT-0002", "999"));
        assertEquals(SeguimientoService.NO_ENCONTRADA, ex.getMessage());
    }

    @Test
    @DisplayName("Un código inexistente da el mismo mensaje")
    void codigoInexistente() {
        RecursoNoEncontradoException ex = assertThrows(RecursoNoEncontradoException.class, () -> service.consultar("OT-9999", "029"));
        assertEquals(SeguimientoService.NO_ENCONTRADA, ex.getMessage());
    }

    @Test
    @DisplayName("Exige al menos 3 caracteres de la placa")
    void placaMuyCorta() {
        assertThrows(RecursoNoEncontradoException.class, () -> service.consultar("OT-0002", "29"));
    }
}
