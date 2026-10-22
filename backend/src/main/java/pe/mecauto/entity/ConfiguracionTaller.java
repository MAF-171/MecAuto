package pe.mecauto.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "configuracion_taller")
@Getter
@Setter
@NoArgsConstructor
public class ConfiguracionTaller {

    @Id
    @Column(name = "clave", length = 40)
    private String clave;

    @Column(name = "valor", length = 300)
    private String valor;

    public ConfiguracionTaller(String clave, String valor) {
        this.clave = clave;
        this.valor = valor;
    }
}
