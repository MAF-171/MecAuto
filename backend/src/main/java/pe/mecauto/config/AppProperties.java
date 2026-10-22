package pe.mecauto.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/** Propiedades "mecauto.*" de application.properties (llegan desde variables de entorno). */
@ConfigurationProperties(prefix = "mecauto")
public record AppProperties(Jwt jwt, Cors cors, AdminInicial adminInicial, Publico publico) {

    public record Jwt(String secret, long expiracionMinutos) {
    }

    public record Cors(List<String> origenes) {
    }

    public record AdminInicial(String username, String password, String nombre) {
    }

    public record Publico(int citasPorVentana, int ventanaMinutos) {
    }
}
