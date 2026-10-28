package pe.mecauto.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import pe.mecauto.domain.Rol;

import java.time.LocalDateTime;

public final class UsuarioDtos {

    private UsuarioDtos() {
    }

    public static final String REGEX_PASSWORD = "^(?=.*[A-Za-z])(?=.*\\d).{10,72}$";
    public static final String MSG_PASSWORD = "La contraseña debe tener entre 10 y 72 caracteres, con letras y números";
    public static final String MSG_CORREO = "Escribe un correo válido";

    public record UsuarioDto(Long id, String username, String nombre, Rol rol, boolean activo, LocalDateTime creadoEn,
                             String correo, String telefono) {
    }

    public record UsuarioCrearRequest(
            @NotBlank(message = "El usuario es obligatorio")
            @Pattern(regexp = "^[a-zA-Z0-9._-]{3,40}$", message = "Usa 3 a 40 caracteres: letras, números, punto, guion o guion bajo")
            String username,
            @NotBlank(message = "El nombre es obligatorio") @Size(max = 100) String nombre,
            @NotNull(message = "Elige un rol") Rol rol,
            @NotBlank(message = "La contraseña es obligatoria") @Pattern(regexp = REGEX_PASSWORD, message = MSG_PASSWORD) String password,
            @Email(message = MSG_CORREO) @Size(max = 100) String correo,
            @Size(max = 20) String telefono) {
    }

    public record UsuarioActualizarRequest(
            @NotBlank(message = "El nombre es obligatorio") @Size(max = 100) String nombre,
            @NotNull(message = "Elige un rol") Rol rol,
            @Email(message = MSG_CORREO) @Size(max = 100) String correo,
            @Size(max = 20) String telefono) {
    }

    public record CambiarPasswordRequest(
            @NotBlank(message = "La contraseña es obligatoria") @Pattern(regexp = REGEX_PASSWORD, message = MSG_PASSWORD) String nuevaPassword) {
    }

    public record EstadoActivoRequest(@NotNull(message = "Indica si el registro está activo") Boolean activo) {
    }
}
