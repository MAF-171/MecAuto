package pe.mecauto.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "secuencia")
@Getter
@Setter
@NoArgsConstructor
public class Secuencia {

    @Id
    @Column(name = "nombre", length = 20)
    private String nombre;

    @Column(name = "ultimo", nullable = false)
    private int ultimo;

    public Secuencia(String nombre, int ultimo) {
        this.nombre = nombre;
        this.ultimo = ultimo;
    }
}
