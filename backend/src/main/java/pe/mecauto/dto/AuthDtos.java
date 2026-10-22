package pe.mecauto.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(
            @NotBlank(message = "Ingresa tu usuario") @Size(max = 40) String username,
            @NotBlank(message = "Ingresa tu contraseña") @Size(max = 100) String password) {
    }

    public record LoginResponse(String token, String tipo, long expiraEnMinutos, UsuarioDtos.UsuarioDto usuario) {
    }
}
