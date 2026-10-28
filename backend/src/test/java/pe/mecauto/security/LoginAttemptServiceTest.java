package pe.mecauto.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.mecauto.exception.DemasiadosIntentosException;

import static org.junit.jupiter.api.Assertions.*;

class LoginAttemptServiceTest {

    private final LoginAttemptService intentos = new LoginAttemptService();

    @Test
    @DisplayName("Tras 5 fallos seguidos el usuario queda bloqueado")
    void bloqueaTrasCincoFallos() {
        for (int i = 0; i < LoginAttemptService.MAX_INTENTOS - 1; i++) {
            intentos.registrarFallo("ana");
        }
        assertDoesNotThrow(() -> intentos.verificarNoBloqueado("ana"));
        intentos.registrarFallo("ana");
        assertThrows(DemasiadosIntentosException.class, () -> intentos.verificarNoBloqueado("ana"));
    }

    @Test
    @DisplayName("El bloqueo no distingue mayúsculas ni espacios y no afecta a otros usuarios")
    void claveNormalizada() {
        for (int i = 0; i < LoginAttemptService.MAX_INTENTOS; i++) {
            intentos.registrarFallo(i % 2 == 0 ? "Ana" : " ana ");
        }
        assertThrows(DemasiadosIntentosException.class, () -> intentos.verificarNoBloqueado("ANA"));
        assertDoesNotThrow(() -> intentos.verificarNoBloqueado("luis"));
    }

    @Test
    @DisplayName("Un ingreso correcto reinicia el contador de fallos")
    void exitoReinicia() {
        for (int i = 0; i < LoginAttemptService.MAX_INTENTOS - 1; i++) {
            intentos.registrarFallo("ana");
        }
        intentos.registrarExito("ana");
        intentos.registrarFallo("ana");
        assertDoesNotThrow(() -> intentos.verificarNoBloqueado("ana"));
    }
}
