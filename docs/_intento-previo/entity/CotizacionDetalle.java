package pe.mecauto.entity;

import jakarta.persistence.*;
import pe.mecauto.entity.enums.TipoLinea;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Línea de una cotización: un servicio o un repuesto con su cantidad y precio. */
@Entity
@Table(name = "cotizacion_detalle")
public class CotizacionDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detalle")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cotizacion", nullable = false)
    private Cotizacion cotizacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 10)
    private TipoLinea tipo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_servicio")
    private Servicio servicio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_repuesto")
    private Repuesto repuesto;

    @Column(name = "descripcion", nullable = false, length = 150)
    private String descripcion;

    @Column(name = "cantidad", nullable = false)
    private int cantidad = 1;

    @Column(name = "precio_unitario", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioUnitario = BigDecimal.ZERO;

    public BigDecimal importe() {
        return precioUnitario.multiply(BigDecimal.valueOf(cantidad)).setScale(2, RoundingMode.HALF_UP);
    }

    // @ACCESSORS
}
