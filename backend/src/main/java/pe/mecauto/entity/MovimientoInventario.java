package pe.mecauto.entity;

import pe.mecauto.util.Tiempo;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.mecauto.domain.TipoMovimiento;

import java.time.LocalDateTime;

@Entity
@Table(name = "movimiento_inventario")
@Getter
@Setter
@NoArgsConstructor
public class MovimientoInventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_movimiento")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_repuesto", nullable = false)
    private Repuesto repuesto;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 10)
    private TipoMovimiento tipo;

    /** Con signo: ENTRADA positiva, SALIDA negativa, AJUSTE positivo o negativo. */
    @Column(name = "cantidad", nullable = false)
    private int cantidad;

    @Column(name = "stock_resultante", nullable = false)
    private int stockResultante;

    @Column(name = "motivo", length = 200)
    private String motivo;

    @Column(name = "id_orden")
    private Long ordenId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    @Column(name = "clave_idempotencia", length = 80)
    private String claveIdempotencia;

    @PrePersist
    void alCrear() {
        if (fecha == null) {
            fecha = Tiempo.ahora();
        }
    }
}
