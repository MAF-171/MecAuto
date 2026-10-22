package pe.mecauto.domain;

import java.util.EnumSet;
import java.util.Set;

/**
 * Estados de la orden de trabajo (Tabla 9 del documento) con sus transiciones válidas (RN02).
 * Es un "enum con comportamiento": cada estado sabe a cuáles puede pasar.
 */
public enum EstadoOrden {
    RECIBIDA, EN_DIAGNOSTICO, EN_REPARACION, COMPLETADA, ENTREGADA, CANCELADA;

    /** Estados a los que se puede pasar desde este (solo avanzar en orden, o cancelar antes de la entrega). */
    public Set<EstadoOrden> siguientes() {
        return switch (this) {
            case RECIBIDA -> EnumSet.of(EN_DIAGNOSTICO, CANCELADA);
            case EN_DIAGNOSTICO -> EnumSet.of(EN_REPARACION, CANCELADA);
            case EN_REPARACION -> EnumSet.of(COMPLETADA, CANCELADA);
            case COMPLETADA -> EnumSet.of(ENTREGADA, CANCELADA);
            case ENTREGADA, CANCELADA -> EnumSet.noneOf(EstadoOrden.class);
        };
    }

    public boolean puedePasarA(EstadoOrden destino) {
        return siguientes().contains(destino);
    }

    /** Mientras la orden está abierta se pueden agregar o quitar servicios y repuestos. */
    public boolean admiteCambiosEnDetalle() {
        return this == RECIBIDA || this == EN_DIAGNOSTICO || this == EN_REPARACION;
    }

    /** Estados "activos": el vehículo sigue en el taller o pendiente de entrega. */
    public boolean estaActiva() {
        return this != ENTREGADA && this != CANCELADA;
    }
}
