package pe.mecauto.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.mecauto.entity.Usuario;
import pe.mecauto.exception.ValidacionException;
import pe.mecauto.repository.UsuarioRepository;
import pe.mecauto.security.LoginAttemptService;
import pe.mecauto.util.TelefonoUtil;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * "Olvidé mi contraseña" del personal: envía un código de 6 dígitos al correo o celular registrado y,
 * con ese código, permite elegir una contraseña nueva.
 * <ul>
 *   <li>La respuesta del paso 1 es siempre la misma: no revela si el correo o celular existe.</li>
 *   <li>El código vence en 15 minutos, admite 5 intentos y se guarda como hash (nunca en texto plano).</li>
 *   <li>Vive en memoria, como el bloqueo de inicio de sesión: un reinicio del servidor anula los códigos.</li>
 * </ul>
 */
@Service
public class RecuperacionPasswordService {

    private static final Logger log = LoggerFactory.getLogger(RecuperacionPasswordService.class);

    static final Duration VIGENCIA = Duration.ofMinutes(15);
    static final int MAX_INTENTOS = 5;
    private static final String CODIGO_INVALIDO = "El código es incorrecto o ya venció. Solicita uno nuevo.";

    private record Pendiente(byte[] codigoHash, Instant expira, int intentos) {
    }

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final EnviadorCodigos enviador;
    private final LoginAttemptService intentosLogin;
    private final SecureRandom random = new SecureRandom();
    private final ConcurrentHashMap<Long, Pendiente> pendientes = new ConcurrentHashMap<>();

    public RecuperacionPasswordService(UsuarioRepository usuarios, PasswordEncoder encoder, EnviadorCodigos enviador,
                                       LoginAttemptService intentosLogin) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.enviador = enviador;
        this.intentosLogin = intentosLogin;
    }

    /** Paso 1. Si el correo o celular pertenece a un usuario activo, le envía un código nuevo. */
    @Transactional(readOnly = true)
    public void solicitar(String identificador) {
        boolean esCorreo = esCorreo(identificador);
        Optional<Usuario> usuario = buscarActivo(identificador);
        if (usuario.isEmpty()) {
            log.info("Recuperación solicitada para un correo/celular no registrado");
            return;
        }
        Usuario u = usuario.get();
        String codigo = "%06d".formatted(random.nextInt(1_000_000));
        pendientes.put(u.getId(), new Pendiente(hash(codigo), Instant.now().plus(VIGENCIA), 0));
        try {
            if (esCorreo) {
                enviador.porCorreo(u.getCorreo(), u.getNombre(), codigo, VIGENCIA.toMinutes());
            } else {
                enviador.porSms(u.getTelefono(), codigo, VIGENCIA.toMinutes());
            }
            log.info("Código de recuperación generado para el usuario '{}' (canal: {})", u.getUsername(), esCorreo ? "correo" : "SMS");
        } catch (RuntimeException e) {
            // La respuesta no cambia (no revela que la cuenta existe); el detalle queda en el log para el administrador.
            log.error("No se pudo enviar el código de recuperación al usuario '{}': {}", u.getUsername(), e.getMessage());
        }
        if (pendientes.size() > 1_000) { // evita crecimiento sin límite
            pendientes.entrySet().removeIf(e -> Instant.now().isAfter(e.getValue().expira()));
        }
    }

    /** Paso 2. Verifica el código y guarda la contraseña nueva. */
    @Transactional
    public void restablecer(String identificador, String codigo, String nuevaPassword) {
        Usuario u = buscarActivo(identificador).orElseThrow(RecuperacionPasswordService::codigoInvalido);
        byte[] recibido = hash(codigo.trim());
        boolean[] correcto = {false};
        pendientes.computeIfPresent(u.getId(), (id, p) -> {
            if (Instant.now().isAfter(p.expira())) {
                return null;
            }
            if (MessageDigest.isEqual(p.codigoHash(), recibido)) {
                correcto[0] = true;
                return null; // un código sirve una sola vez
            }
            int intentos = p.intentos() + 1;
            return intentos >= MAX_INTENTOS ? null : new Pendiente(p.codigoHash(), p.expira(), intentos);
        });
        if (!correcto[0]) {
            throw codigoInvalido();
        }
        u.setPasswordHash(encoder.encode(nuevaPassword));
        intentosLogin.registrarExito(u.getUsername()); // quita un posible bloqueo por intentos fallidos
        log.info("Contraseña restablecida con código para el usuario '{}'", u.getUsername());
    }

    private Optional<Usuario> buscarActivo(String identificador) {
        String texto = identificador == null ? "" : identificador.trim();
        Optional<Usuario> u;
        if (esCorreo(texto)) {
            u = usuarios.findByCorreo(texto.toLowerCase(Locale.ROOT));
        } else {
            String telefono = TelefonoUtil.normalizar(texto);
            if (telefono == null) {
                throw new ValidacionException("identificador", "Escribe un correo o un número de celular válido");
            }
            u = usuarios.findByTelefono(telefono);
        }
        return u.filter(Usuario::isActivo);
    }

    private static boolean esCorreo(String texto) {
        return texto != null && texto.contains("@");
    }

    private static ValidacionException codigoInvalido() {
        return new ValidacionException("codigo", CODIGO_INVALIDO);
    }

    private static byte[] hash(String codigo) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(codigo.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
