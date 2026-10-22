package pe.mecauto.entity.enums;

/** Estados de una cita. */
public enum EstadoCita {
    PENDIENTE,
    CONFIRMADA,
    ATENDIDA,
    CANCELADA,
    NO_ASISTIO;

    /** Una cita activa ocupa un cupo del horario. */
    public boolean ocupaCupo() {
        return this == PENDIENTE || this == CONFIRMADA;
    }
}
