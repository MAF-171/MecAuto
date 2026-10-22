package pe.mecauto.entity.enums;

/**
 * Estados de una orden de trabajo (Tabla 9 y regla RN02 del documento).
 *
 * <p>Una orden solo avanza en este orden: RECIBIDA, EN_DIAGNOSTICO, EN_REPARACION,
 * COMPLETADA y ENTREGADA. Puede cancelarse antes de la entrega. ENTREGADA y CANCELADA
 * son estados finales. Esta clase no depende de Spring ni de la base de datos, por lo
 * que sus reglas se prueban con pruebas unitarias simples.</p>
 */
public enum EstadoOrden {
    RECIBIDA("Recibida"),
    EN_DIAGNOSTICO("En diagnóstico"),
    EN_REPARACION("En reparación"),
    COMPLETADA("Completada"),
    ENTREGADA("Entregada"),
    CANCELADA("Cancelada");

    private final String etiqueta;

    EstadoOrden(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    /** Estados finales: ya no admiten ningún cambio. */
    public boolean esFinal() {
        return this == ENTREGADA || this == CANCELADA;
    }

    /** En estos estados todavía se pueden agregar o quitar servicios, repuestos y mano de obra. */
    public boolean permiteEditarDetalle() {
        return this == RECIBIDA || this == EN_DIAGNOSTICO || this == EN_REPARACION;
    }

    /**
     * Indica si es válido pasar del estado actual al estado de destino.
     * Solo se permite avanzar un paso, o cancelar desde cualquier estado no final.
     */
    public boolean puedePasarA(EstadoOrden destino) {
        if (destino == null || esFinal()) {
            return false;
        }
        if (destino == CANCELADA) {
            return true;
        }
        return destino.ordinal() == this.ordinal() + 1;
    }
}
