package pe.mecauto.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrdenEstadoPolicyTest {

    @Test
    @DisplayName("El administrador puede pasar a cualquier estado")
    void administrador() {
        for (EstadoOrden e : EstadoOrden.values()) {
            assertTrue(OrdenEstadoPolicy.puedeCambiarA(Rol.ADMINISTRADOR, e), e.name());
        }
    }

    @Test
    @DisplayName("El mecánico diagnostica, repara y completa, pero no entrega ni cancela")
    void mecanico() {
        assertTrue(OrdenEstadoPolicy.puedeCambiarA(Rol.MECANICO, EstadoOrden.EN_DIAGNOSTICO));
        assertTrue(OrdenEstadoPolicy.puedeCambiarA(Rol.MECANICO, EstadoOrden.EN_REPARACION));
        assertTrue(OrdenEstadoPolicy.puedeCambiarA(Rol.MECANICO, EstadoOrden.COMPLETADA));
        assertFalse(OrdenEstadoPolicy.puedeCambiarA(Rol.MECANICO, EstadoOrden.ENTREGADA));
        assertFalse(OrdenEstadoPolicy.puedeCambiarA(Rol.MECANICO, EstadoOrden.CANCELADA));
    }

    @Test
    @DisplayName("Recepción completa y entrega, pero no diagnostica ni cancela")
    void recepcion() {
        assertTrue(OrdenEstadoPolicy.puedeCambiarA(Rol.RECEPCION, EstadoOrden.COMPLETADA));
        assertTrue(OrdenEstadoPolicy.puedeCambiarA(Rol.RECEPCION, EstadoOrden.ENTREGADA));
        assertFalse(OrdenEstadoPolicy.puedeCambiarA(Rol.RECEPCION, EstadoOrden.EN_DIAGNOSTICO));
        assertFalse(OrdenEstadoPolicy.puedeCambiarA(Rol.RECEPCION, EstadoOrden.EN_REPARACION));
        assertFalse(OrdenEstadoPolicy.puedeCambiarA(Rol.RECEPCION, EstadoOrden.CANCELADA));
    }
}
