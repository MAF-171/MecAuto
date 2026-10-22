package pe.mecauto.entity.enums;

/** Tipos de movimiento de inventario. La cantidad siempre es positiva; el tipo indica si suma o resta. */
public enum TipoMovimiento {
    ENTRADA(1),
    SALIDA(-1),
    AJUSTE_ENTRADA(1),
    AJUSTE_SALIDA(-1),
    DEVOLUCION(1);

    private final int signo;

    TipoMovimiento(int signo) {
        this.signo = signo;
    }

    /** +1 si aumenta el stock, -1 si lo disminuye. */
    public int getSigno() {
        return signo;
    }
}
