package pe.mecauto.entity;

import pe.mecauto.util.Tiempo;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "vehiculo")
@Getter
@Setter
@NoArgsConstructor
public class Vehiculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_vehiculo")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cliente", nullable = false)
    private Cliente cliente;

    @Column(name = "placa", nullable = false, length = 10, unique = true)
    private String placa;

    @Column(name = "marca", nullable = false, length = 40)
    private String marca;

    @Column(name = "modelo", nullable = false, length = 40)
    private String modelo;

    @Column(name = "anio")
    private Integer anio;

    @Column(name = "color", length = 30)
    private String color;

    @Column(name = "kilometraje", nullable = false)
    private int kilometraje;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @PrePersist
    void alCrear() {
        if (creadoEn == null) {
            creadoEn = Tiempo.ahora();
        }
    }
}
