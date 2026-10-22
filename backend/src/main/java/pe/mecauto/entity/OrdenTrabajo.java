package pe.mecauto.entity;

import pe.mecauto.util.Tiempo;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.mecauto.domain.CalculadoraIgv;
import pe.mecauto.domain.EstadoOrden;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orden_trabajo")
@Getter
@Setter
@NoArgsConstructor
public class OrdenTrabajo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_orden")
    private Long id;

    @Column(name = "codigo", nullable = false, length = 12, unique = true)
    private String codigo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cliente", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_vehiculo", nullable = false)
    private Vehiculo vehiculo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_mecanico")
    private Usuario mecanico;

    @Column(name = "id_cita")
    private Long citaId;

    @Column(name = "falla", nullable = false, length = 500)
    private String falla;

    @Column(name = "diagnostico", length = 1000)
    private String diagnostico;

    @Column(name = "trabajos_realizados", length = 1000)
    private String trabajosRealizados;

    @Column(name = "kilometraje_ingreso")
    private Integer kilometrajeIngreso;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 15)
    private EstadoOrden estado = EstadoOrden.RECIBIDA;

    @Column(name = "fecha_ingreso", nullable = false)
    private LocalDateTime fechaIngreso;

    @Column(name = "fecha_entrega_est")
    private LocalDate fechaEntregaEstimada;

    @Column(name = "fecha_cierre")
    private LocalDateTime fechaCierre;

    /** Mano de obra con IGV incluido. */
    @Column(name = "mano_obra", nullable = false, precision = 12, scale = 2)
    private BigDecimal manoObra = BigDecimal.ZERO;

    /** Descuento en soles (con IGV incluido) que se resta del total, por ejemplo el de una cotización. */
    @Column(name = "descuento", nullable = false, precision = 12, scale = 2)
    private BigDecimal descuento = BigDecimal.ZERO;

    /** Total con IGV incluido = servicios + repuestos + mano de obra - descuento. */
    @Column(name = "total", nullable = false, precision = 12, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_creado_por")
    private Usuario creadoPor;

    @Version
    @Column(name = "version", nullable = false)
    private int version;

    @OneToMany(mappedBy = "orden", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<DetalleOrdenServicio> servicios = new ArrayList<>();

    @OneToMany(mappedBy = "orden", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<DetalleOrdenRepuesto> repuestos = new ArrayList<>();

    @PrePersist
    void alCrear() {
        if (fechaIngreso == null) {
            fechaIngreso = Tiempo.ahora();
        }
    }

    /** Recalcula el total a partir de las líneas y la mano de obra (todo con IGV incluido). */
    public void recalcularTotal() {
        BigDecimal suma = manoObra == null ? BigDecimal.ZERO : manoObra;
        for (DetalleOrdenServicio s : servicios) {
            suma = suma.add(CalculadoraIgv.importe(s.getCantidad(), s.getPrecioUnitario()));
        }
        for (DetalleOrdenRepuesto r : repuestos) {
            suma = suma.add(CalculadoraIgv.importe(r.getCantidad(), r.getPrecioUnitario()));
        }
        BigDecimal desc = descuento == null ? BigDecimal.ZERO : descuento;
        if (desc.compareTo(suma) > 0) {
            desc = suma; // el descuento nunca puede superar el monto
        }
        this.descuento = CalculadoraIgv.redondear(desc);
        this.total = CalculadoraIgv.redondear(suma.subtract(desc));
    }
}
