package pe.mecauto.security;

import org.springframework.stereotype.Service;
import pe.mecauto.exception.DemasiadosIntentosException;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Protección contra adivinar contraseñas: tras 5 intentos fallidos seguidos de un mismo usuario,
 * se bloquea el inicio de sesión de ese usuario por 15 minutos. Vive en memoria (se reinicia con el servidor).
 */
@Service
public class LoginAttemptService {

    static final int MAX_INTENTOS = 5;
    static final Duration BLOQUEO = Duration.ofMinutes(15);

    private record Estado(int fallos, Instant bloqueadoHasta) {
    }

    private final ConcurrentHashMap<String, Estado> intentos = new ConcurrentHashMap<>();

    private String clave(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }

    public void verificarNoBloqueado(String username) {
        Estado e = intentos.get(clave(username));
        if (e != null && e.bloqueadoHasta() != null && Instant.now().isBefore(e.bloqueadoHasta())) {
            long min = Math.max(1, Duration.between(Instant.now(), e.bloqueadoHasta()).toMinutes() + 1);
            throw new DemasiadosIntentosException("Demasiados intentos fallidos. Intenta de nuevo en " + min + " minuto(s).");
        }
    }

    public void registrarFallo(String username) {
        intentos.merge(clave(username), new Estado(1, null), (actual, nuevo) -> {
            if (actual.bloqueadoHasta() != null && Instant.now().isAfter(actual.bloqueadoHasta())) {
                return new Estado(1, null);
            }
            int fallos = actual.fallos() + 1;
            return fallos >= MAX_INTENTOS ? new Estado(fallos, Instant.now().plus(BLOQUEO)) : new Estado(fallos, null);
        });
    }

    public void registrarExito(String username) {
        intentos.remove(clave(username));
    }
}
