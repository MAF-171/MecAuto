package pe.mecauto.domain;

/**
 * Regla de quién puede llevar una orden a cada estado (Tabla 9, columna "Quién lo actualiza").
 * Clase sin dependencias de Spring: se puede probar sola.
 *
 * - EN_DIAGNOSTICO / EN_REPARACION: mecánico (y administrador)
 * - COMPLETADA: mecánico o recepción (y administrador)
 * - ENTREGADA: recepción (y administrador)
 * - CANCELADA: solo administrador
 */
public final class OrdenEstadoPolicy {

    private OrdenEstadoPolicy() {
    }

    public static boolean puedeCambiarA(Rol rol, EstadoOrden destino) {
        if (rol == Rol.ADMINISTRADOR) {
            return true;
        }
        return switch (destino) {
            case EN_DIAGNOSTICO, EN_REPARACION -> rol == Rol.MECANICO;
            case COMPLETADA -> rol == Rol.MECANICO || rol == Rol.RECEPCION;
            case ENTREGADA -> rol == Rol.RECEPCION;
            case CANCELADA, RECIBIDA -> false;
        };
    }
}
