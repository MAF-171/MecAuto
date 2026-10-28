package pe.mecauto.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import pe.mecauto.domain.Rol;
import pe.mecauto.entity.Usuario;
import pe.mecauto.exception.ValidacionException;
import pe.mecauto.repository.UsuarioRepository;
import pe.mecauto.security.LoginAttemptService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Recuperación de contraseña con repositorio y envío simulados (no requiere SQL Server ni correo). */
@ExtendWith(MockitoExtension.class)
class RecuperacionPasswordServiceTest {

    @Mock UsuarioRepository usuarios;
    @Mock PasswordEncoder encoder;
    @Mock EnviadorCodigos enviador;
    @Mock LoginAttemptService intentosLogin;

    RecuperacionPasswordService service;
    Usuario ana;

    @BeforeEach
    void preparar() {
        service = new RecuperacionPasswordService(usuarios, encoder, enviador, intentosLogin);
        ana = new Usuario();
        ana.setId(7L);
        ana.setUsername("ana");
        ana.setNombre("Ana Ríos");
        ana.setRol(Rol.RECEPCION);
        ana.setPasswordHash("hash-viejo");
        ana.setCorreo("ana@taller.pe");
        ana.setTelefono("+51987654321");
    }

    private String pedirCodigoPorCorreo() {
        when(usuarios.findByCorreo("ana@taller.pe")).thenReturn(Optional.of(ana));
        service.solicitar("  ANA@taller.pe ");
        ArgumentCaptor<String> codigo = ArgumentCaptor.forClass(String.class);
        verify(enviador).porCorreo(eq("ana@taller.pe"), eq("Ana Ríos"), codigo.capture(), anyLong());
        return codigo.getValue();
    }

    @Test
    @DisplayName("Con el código correcto cambia la contraseña y quita el bloqueo de login")
    void codigoCorrectoCambiaPassword() {
        String codigo = pedirCodigoPorCorreo();
        assertTrue(codigo.matches("\\d{6}"));
        when(encoder.encode("NuevaClave2024")).thenReturn("hash-nuevo");

        service.restablecer("ana@taller.pe", codigo, "NuevaClave2024");

        assertEquals("hash-nuevo", ana.getPasswordHash());
        verify(intentosLogin).registrarExito("ana");
    }

    @Test
    @DisplayName("El código sirve una sola vez")
    void codigoDeUnSoloUso() {
        String codigo = pedirCodigoPorCorreo();
        when(encoder.encode(any())).thenReturn("hash-nuevo");
        service.restablecer("ana@taller.pe", codigo, "NuevaClave2024");

        assertThrows(ValidacionException.class, () -> service.restablecer("ana@taller.pe", codigo, "OtraClave2024"));
    }

    @Test
    @DisplayName("Tras 5 códigos errados el código se anula aunque luego se escriba bien")
    void bloqueaTrasIntentosFallidos() {
        String codigo = pedirCodigoPorCorreo();
        String errado = codigo.equals("000000") ? "111111" : "000000";
        for (int i = 0; i < RecuperacionPasswordService.MAX_INTENTOS; i++) {
            assertThrows(ValidacionException.class, () -> service.restablecer("ana@taller.pe", errado, "NuevaClave2024"));
        }
        assertThrows(ValidacionException.class, () -> service.restablecer("ana@taller.pe", codigo, "NuevaClave2024"));
        assertEquals("hash-viejo", ana.getPasswordHash());
    }

    @Test
    @DisplayName("Por celular envía SMS al número normalizado")
    void porCelularEnviaSms() {
        when(usuarios.findByTelefono("+51987654321")).thenReturn(Optional.of(ana));
        service.solicitar("987 654 321");
        verify(enviador).porSms(eq("+51987654321"), anyString(), anyLong());
        verify(enviador, never()).porCorreo(any(), any(), any(), anyLong());
    }

    @Test
    @DisplayName("Un correo no registrado o un usuario inactivo no recibe nada y no da error")
    void noRevelaSiExiste() {
        when(usuarios.findByCorreo("nadie@taller.pe")).thenReturn(Optional.empty());
        assertDoesNotThrow(() -> service.solicitar("nadie@taller.pe"));

        ana.setActivo(false);
        when(usuarios.findByCorreo("ana@taller.pe")).thenReturn(Optional.of(ana));
        assertDoesNotThrow(() -> service.solicitar("ana@taller.pe"));

        verifyNoInteractions(enviador);
    }

    @Test
    @DisplayName("Si falla el envío la respuesta es la misma")
    void falloDeEnvioNoSeRevela() {
        when(usuarios.findByCorreo("ana@taller.pe")).thenReturn(Optional.of(ana));
        doThrow(new RuntimeException("SMTP caído")).when(enviador).porCorreo(any(), any(), any(), anyLong());
        assertDoesNotThrow(() -> service.solicitar("ana@taller.pe"));
    }

    @Test
    @DisplayName("Un texto que no es correo ni celular se rechaza")
    void identificadorInvalido() {
        assertThrows(ValidacionException.class, () -> service.solicitar("hola"));
    }
}
