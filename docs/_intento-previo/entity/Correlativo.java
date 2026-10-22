package pe.mecauto.entity;

import jakarta.persistence.*;

/** Último número usado por cada tipo de documento (OT, COT, CIT, B001, F001). */
@Entity
@Table(name = "correlativo")
public class Correlativo {

    @Id
    @Column(name = "clave", length = 20)
    private String clave;

    @Column(name = "ultimo_numero", nullable = false)
    private int ultimoNumero;

    public Correlativo() {
    }

    public Correlativo(String clave, int ultimoNumero) {
        this.clave = clave;
        this.ultimoNumero = ultimoNumero;
    }

    // @ACCESSORS
}
