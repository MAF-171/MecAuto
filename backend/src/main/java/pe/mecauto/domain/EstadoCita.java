package pe.mecauto.domain;

public enum EstadoCita {
    PENDIENTE, CONFIRMADA, COMPLETADA, CANCELADA;

    public boolean estaAbierta() {
        return this == PENDIENTE || this == CONFIRMADA;
    }
}
