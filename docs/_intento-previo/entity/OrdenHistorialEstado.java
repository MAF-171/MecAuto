package pe.mecauto.entity;

import jakarta.persistence.*;
import pe.mecauto.entity.enums.EstadoOrden;
import pe.mecauto.util.Tiempo;

import java.time.LocalDateTime;

/** Registro de cada cambio de estado de una orden (quién, cuándo y por qué). */
@Entity
@Table(name = "orden_historial_estado")
public class OrdenHistorialEstado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_historial")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_orden", nullable = false)
    private OrdenTrabajo orden;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior", length = 15)
    private EstadoOrden estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_nuevo", nullable = false, length = 15)
    private EstadoOrden estadoNuevo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @Column(name = "comentario", length = 300)
    private String comentario;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha = Tiempo.ahora();

    // @ACCESSORS
}
