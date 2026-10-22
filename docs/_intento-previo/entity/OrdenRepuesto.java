package pe.mecauto.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Repuesto anotado en una orden de trabajo.
 * Si consumido es verdadero, el stock ya se descontó (hay un movimiento de SALIDA).
 * Si es falso, es un repuesto planificado que todavía no descuenta stock.
 */
@Entity
@Table(name = "orden_repuesto")
public class OrdenRepuesto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detalle")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_orden", nullable = false)
    private OrdenTrabajo orden;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_repuesto", nullable = false)
    private Repuesto repuesto;

    @Column(name = "cantidad", nullable = false)
    private int cantidad = 1;

    @Column(name = "precio_unitario", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioUnitario = BigDecimal.ZERO;

    @Column(name = "consumido", nullable = false)
    private boolean consumido;

    @Column(name = "fecha_consumo")
    private LocalDateTime fechaConsumo;

    @Column(name = "clave_idempotencia", length = 60)
    private String claveIdempotencia;

    public BigDecimal importe() {
        return precioUnitario.multiply(BigDecimal.valueOf(cantidad)).setScale(2, RoundingMode.HALF_UP);
    }

    // @ACCESSORS
}
