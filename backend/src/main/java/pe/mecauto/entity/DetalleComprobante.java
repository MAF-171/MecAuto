package pe.mecauto.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "detalle_comprobante")
@Getter
@Setter
@NoArgsConstructor
public class DetalleComprobante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detalle")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_comprobante", nullable = false)
    private Comprobante comprobante;

    @Column(name = "descripcion", nullable = false, length = 200)
    private String descripcion;

    @Column(name = "cantidad", nullable = false)
    private int cantidad;

    @Column(name = "precio_unitario", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioUnitario;

    @Column(name = "importe", nullable = false, precision = 12, scale = 2)
    private BigDecimal importe;
}
