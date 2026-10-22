package pe.mecauto.entity;

import pe.mecauto.util.Tiempo;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.mecauto.domain.EstadoComprobante;
import pe.mecauto.domain.MetodoPago;
import pe.mecauto.domain.TipoComprobante;
import pe.mecauto.domain.TipoDocumento;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Comprobante INTERNO con formato de boleta/factura. No es un comprobante electrónico autorizado por SUNAT. */
@Entity
@Table(name = "comprobante")
@Getter
@Setter
@NoArgsConstructor
public class Comprobante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_comprobante")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_orden", nullable = false)
    private OrdenTrabajo orden;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 7)
    private TipoComprobante tipo;

    @Column(name = "serie", nullable = false, length = 4)
    private String serie;

    @Column(name = "numero", nullable = false, length = 8)
    private String numero;

    @Column(name = "cliente_nombre", nullable = false, length = 120)
    private String clienteNombre;

    @Enumerated(EnumType.STRING)
    @Column(name = "cliente_tipo_doc", nullable = false, length = 3)
    private TipoDocumento clienteTipoDoc;

    @Column(name = "cliente_documento", nullable = false, length = 12)
    private String clienteDocumento;

    @Column(name = "vehiculo_placa", length = 10)
    private String vehiculoPlaca;

    @Column(name = "base_imponible", nullable = false, precision = 12, scale = 2)
    private BigDecimal baseImponible;

    @Column(name = "igv", nullable = false, precision = 12, scale = 2)
    private BigDecimal igv;

    @Column(name = "total", nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pago", nullable = false, length = 15)
    private MetodoPago metodoPago;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 8)
    private EstadoComprobante estado = EstadoComprobante.EMITIDO;

    @Column(name = "motivo_anulacion", length = 200)
    private String motivoAnulacion;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDateTime fechaEmision;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_emitido_por")
    private Usuario emitidoPor;

    @OneToMany(mappedBy = "comprobante", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<DetalleComprobante> detalles = new ArrayList<>();

    @PrePersist
    void alCrear() {
        if (fechaEmision == null) {
            fechaEmision = Tiempo.ahora();
        }
    }

    public String numeroCompleto() {
        return serie + "-" + numero;
    }
}
