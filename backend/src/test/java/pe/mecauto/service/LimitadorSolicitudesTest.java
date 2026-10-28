package pe.mecauto.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.mecauto.config.AppProperties;
import pe.mecauto.exception.DemasiadosIntentosException;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class LimitadorSolicitudesTest {

    private final LimitadorSolicitudes limitador =
            new LimitadorSolicitudes(new AppProperties(null, null, null, new AppProperties.Publico(3, 10), null));

    @Test
    @DisplayName("Permite hasta el máximo configurado y luego bloquea")
    void bloqueaAlSuperarElMaximo() {
        for (int i = 0; i < 3; i++) {
            assertDoesNotThrow(() -> limitador.verificar("1.1.1.1"));
        }
        assertThrows(DemasiadosIntentosException.class, () -> limitador.verificar("1.1.1.1"));
    }

    @Test
    @DisplayName("Cada IP y cada operación cuentan por separado")
    void contadoresSeparados() {
        for (int i = 0; i < 3; i++) {
            limitador.verificar("1.1.1.1");
        }
        assertDoesNotThrow(() -> limitador.verificar("2.2.2.2"));
        assertDoesNotThrow(() -> limitador.verificar("consulta:1.1.1.1", 15, Duration.ofMinutes(10)));
    }

    @Test
    @DisplayName("Los intentos fuera de la ventana de tiempo ya no cuentan")
    void ventanaDeslizante() throws InterruptedException {
        Duration ventana = Duration.ofMillis(50);
        limitador.verificar("x", 1, ventana);
        assertThrows(DemasiadosIntentosException.class, () -> limitador.verificar("x", 1, ventana));
        Thread.sleep(80);
        assertDoesNotThrow(() -> limitador.verificar("x", 1, ventana));
    }
}
