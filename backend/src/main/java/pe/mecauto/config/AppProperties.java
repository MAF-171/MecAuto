package pe.mecauto.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/** Propiedades "mecauto.*" de application.properties (llegan desde variables de entorno). */
@ConfigurationProperties(prefix = "mecauto")
public record AppProperties(Jwt jwt, Cors cors, AdminInicial adminInicial, Publico publico,
                            Recuperacion recuperacion) {

    public record Jwt(String secret, long expiracionMinutos) {
    }

    public record Cors(List<String> origenes) {
    }

    public record AdminInicial(String username, String password, String nombre) {
    }

    public record Publico(int citasPorVentana, int ventanaMinutos) {
    }

    /**
     * Recuperación de contraseña. El código se envía por correo (spring.mail.*) o por SMS (Twilio).
     * codigoEnLog=true escribe el código en el log cuando no hay canal configurado: SOLO para desarrollo.
     */
    public record Recuperacion(String remitente, boolean codigoEnLog, String twilioSid, String twilioToken,
                               String twilioNumero) {
    }
}
