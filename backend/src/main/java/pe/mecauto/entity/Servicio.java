package pe.mecauto.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "servicio")
@Getter
@Setter
@NoArgsConstructor
public class Servicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_servicio")
    private Long id;

    @Column(name = "nombre", nullable = false, length = 100, unique = true)
    private String nombre;

    @Column(name = "descripcion", length = 500)
    private String descripcion;

    /** Precio con IGV incluido. null = "según diagnóstico". */
    @Column(name = "precio_referencial", precision = 12, scale = 2)
    private BigDecimal precioReferencial;

    @Column(name = "duracion_minutos")
    private Integer duracionMinutos;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;
}
