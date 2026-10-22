package pe.mecauto.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EstadoOrdenTest {

    @Test
    @DisplayName("Flujo normal permitido paso a paso")
    void flujoNormal() {
        assertTrue(EstadoOrden.RECIBIDA.puedePasarA(EstadoOrden.EN_DIAGNOSTICO));
        assertTrue(EstadoOrden.EN_DIAGNOSTICO.puedePasarA(EstadoOrden.EN_REPARACION));
        assertTrue(EstadoOrden.EN_REPARACION.puedePasarA(EstadoOrden.COMPLETADA));
        assertTrue(EstadoOrden.COMPLETADA.puedePasarA(EstadoOrden.ENTREGADA));
    }

    @Test
    @DisplayName("No se permiten saltos ni retrocesos")
    void sinSaltosNiRetrocesos() {
        assertFalse(EstadoOrden.RECIBIDA.puedePasarA(EstadoOrden.COMPLETADA));
        assertFalse(EstadoOrden.RECIBIDA.puedePasarA(EstadoOrden.ENTREGADA));
        assertFalse(EstadoOrden.EN_REPARACION.puedePasarA(EstadoOrden.EN_DIAGNOSTICO));
        assertFalse(EstadoOrden.COMPLETADA.puedePasarA(EstadoOrden.EN_REPARACION));
    }

    @Test
    @DisplayName("ENTREGADA y CANCELADA son estados finales")
    void estadosFinales() {
        assertTrue(EstadoOrden.ENTREGADA.siguientes().isEmpty());
        assertTrue(EstadoOrden.CANCELADA.siguientes().isEmpty());
        assertFalse(EstadoOrden.ENTREGADA.estaActiva());
        assertFalse(EstadoOrden.CANCELADA.estaActiva());
    }

    @Test
    @DisplayName("Se puede cancelar desde cualquier estado no final")
    void cancelacion() {
        for (EstadoOrden e : new EstadoOrden[]{EstadoOrden.RECIBIDA, EstadoOrden.EN_DIAGNOSTICO,
                EstadoOrden.EN_REPARACION, EstadoOrden.COMPLETADA}) {
            assertTrue(e.puedePasarA(EstadoOrden.CANCELADA), e.name());
        }
    }

    @Test
    @DisplayName("El detalle solo se edita antes de completar la orden")
    void edicionDeDetalle() {
        assertTrue(EstadoOrden.RECIBIDA.admiteCambiosEnDetalle());
        assertTrue(EstadoOrden.EN_REPARACION.admiteCambiosEnDetalle());
        assertFalse(EstadoOrden.COMPLETADA.admiteCambiosEnDetalle());
        assertFalse(EstadoOrden.CANCELADA.admiteCambiosEnDetalle());
    }
}
