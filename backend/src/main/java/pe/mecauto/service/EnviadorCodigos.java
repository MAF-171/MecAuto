package pe.mecauto.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import pe.mecauto.config.AppProperties;

/**
 * Envía el código de recuperación de contraseña por correo (SMTP) o por SMS (API REST de Twilio).
 * Si el canal no está configurado, no envía nada y lo deja anotado en el log.
 */
@Component
public class EnviadorCodigos {

    private static final Logger log = LoggerFactory.getLogger(EnviadorCodigos.class);
    private static final String TWILIO_URL = "https://api.twilio.com/2010-04-01/Accounts/{sid}/Messages.json";

    private final ObjectProvider<JavaMailSender> correo;
    private final AppProperties.Recuperacion cfg;
    private final String hostCorreo;
    private final String usuarioCorreo;
    private final RestClient http = RestClient.create();

    public EnviadorCodigos(ObjectProvider<JavaMailSender> correo, AppProperties props, Environment env) {
        this.correo = correo;
        this.cfg = props.recuperacion();
        this.hostCorreo = env.getProperty("spring.mail.host", "");
        this.usuarioCorreo = env.getProperty("spring.mail.username", "");
    }

    public void porCorreo(String destino, String nombre, String codigo, long minutos) {
        JavaMailSender sender = correo.getIfAvailable();
        if (hostCorreo.isBlank() || sender == null) {
            sinCanal("correo", "MECAUTO_MAIL_HOST", codigo);
            return;
        }
        SimpleMailMessage m = new SimpleMailMessage();
        m.setFrom(cfg.remitente() == null || cfg.remitente().isBlank() ? usuarioCorreo : cfg.remitente());
        m.setTo(destino);
        m.setSubject("MecAuto - Código para recuperar tu contraseña");
        m.setText("Hola " + nombre + ",\n\n"
                + "Tu código para crear una nueva contraseña en MecAuto es: " + codigo + "\n\n"
                + "Vence en " + minutos + " minutos. Si no lo pediste, ignora este mensaje: tu contraseña sigue igual.\n");
        sender.send(m);
    }

    public void porSms(String telefono, String codigo, long minutos) {
        if (vacio(cfg.twilioSid()) || vacio(cfg.twilioToken()) || vacio(cfg.twilioNumero())) {
            sinCanal("SMS", "MECAUTO_TWILIO_SID", codigo);
            return;
        }
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("To", telefono);
        form.add("From", cfg.twilioNumero());
        form.add("Body", "MecAuto: tu código para recuperar la contraseña es " + codigo + ". Vence en " + minutos + " min.");
        http.post()
                .uri(TWILIO_URL, cfg.twilioSid())
                .headers(h -> h.setBasicAuth(cfg.twilioSid(), cfg.twilioToken()))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .toBodilessEntity();
    }

    private void sinCanal(String canal, String variable, String codigo) {
        if (cfg.codigoEnLog()) {
            log.warn("[SOLO DESARROLLO] {} no configurado ({}). Código de recuperación: {}", canal, variable, codigo);
        } else {
            log.warn("No se envió el código de recuperación: {} no configurado ({})", canal, variable);
        }
    }

    private static boolean vacio(String s) {
        return s == null || s.isBlank();
    }
}
