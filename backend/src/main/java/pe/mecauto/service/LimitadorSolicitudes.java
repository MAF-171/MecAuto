package pe.mecauto.service;

import org.springframework.stereotype.Component;
import pe.mecauto.config.AppProperties;
import pe.mecauto.exception.DemasiadosIntentosException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limitador simple de "ventana deslizante" por IP para el formulario público de citas (anti-spam).
 * Vive en memoria: se reinicia con el servidor y cada instancia cuenta por separado.
 */
@Component
public class LimitadorSolicitudes {

    private final int maximo;
    private final Duration ventana;
    private final ConcurrentHashMap<String, Deque<Instant>> registros = new ConcurrentHashMap<>();

    public LimitadorSolicitudes(AppProperties props) {
        this.maximo = props.publico().citasPorVentana();
        this.ventana = Duration.ofMinutes(props.publico().ventanaMinutos());
    }

    /** Límite del formulario de citas (valores de application.properties). */
    public void verificar(String clave) {
        verificar(clave, maximo, ventana);
    }

    /** Límite propio para otras operaciones públicas; usa una clave distinta para no mezclar contadores. */
    public void verificar(String clave, int maximoPermitido, Duration ventanaPermitida) {
        Instant ahora = Instant.now();
        Deque<Instant> cola = registros.computeIfAbsent(clave, k -> new ArrayDeque<>());
        synchronized (cola) {
            while (!cola.isEmpty() && cola.peekFirst().isBefore(ahora.minus(ventanaPermitida))) {
                cola.pollFirst();
            }
            if (cola.size() >= maximoPermitido) {
                throw new DemasiadosIntentosException("Has hecho muchos intentos. Inténtalo de nuevo en unos minutos.");
            }
            cola.addLast(ahora);
        }
        if (registros.size() > 10_000) { // evita crecimiento sin límite
            registros.entrySet().removeIf(e -> e.getValue().isEmpty());
        }
    }
}
