package pe.mecauto.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(
            @NotBlank(message = "Ingresa tu usuario") @Size(max = 40) String username,
            @NotBlank(message = "Ingresa tu contraseña") @Size(max = 100) String password) {
    }

    /** Paso 1: la persona escribe el correo o el celular registrado en su usuario. */
    public record RecuperarRequest(
            @NotBlank(message = "Escribe tu correo o tu número de celular") @Size(max = 100) String identificador) {
    }

    /** Paso 2: código recibido + contraseña nueva. */
    public record RestablecerRequest(
            @NotBlank(message = "Escribe tu correo o tu número de celular") @Size(max = 100) String identificador,
            @NotBlank(message = "Escribe el código que recibiste")
            @Pattern(regexp = "^\\d{6}$", message = "El código tiene 6 dígitos") String codigo,
            @NotBlank(message = "La contraseña es obligatoria")
            @Pattern(regexp = UsuarioDtos.REGEX_PASSWORD, message = UsuarioDtos.MSG_PASSWORD) String nuevaPassword) {
    }

    public record LoginResponse(String token, String tipo, long expiraEnMinutos, UsuarioDtos.UsuarioDto usuario) {
    }
}
