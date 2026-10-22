package pe.mecauto.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import pe.mecauto.domain.EstadoCita;
import pe.mecauto.domain.OrigenCita;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class CitaDtos {

    private CitaDtos() {
    }

    public record CitaDto(Long id, String codigo, Long clienteId, String clienteNombre, Long vehiculoId,
                          Long servicioId, String servicioNombre, String nombreContacto, String telefonoContacto,
                          String correoContacto, String placa, String marca, String modelo, LocalDateTime fechaHora,
                          String motivo, EstadoCita estado, OrigenCita origen, LocalDateTime creadoEn) {
    }

    /** Alta o edición interna (recepción). cliente y vehículo son opcionales hasta vincularlos. */
    public record CitaRequest(
            Long clienteId,
            Long vehiculoId,
            @NotNull(message = "Elige el servicio") Long servicioId,
            @NotBlank(message = "El nombre de contacto es obligatorio") @Size(max = 120) String nombreContacto,
            @NotBlank(message = "El teléfono es obligatorio") @Pattern(regexp = "^[0-9+()\\- ]{6,20}$", message = "Teléfono inválido") String telefonoContacto,
            @Email(message = "Correo inválido") @Size(max = 100) String correoContacto,
            @NotBlank(message = "La placa es obligatoria") @Size(max = 10) String placa,
            @Size(max = 40) String marca,
            @Size(max = 40) String modelo,
            @NotNull(message = "Elige fecha y hora") LocalDateTime fechaHora,
            @Size(max = 300) String motivo) {
    }

    public record ReprogramarRequest(@NotNull(message = "Elige la nueva fecha y hora") LocalDateTime fechaHora) {
    }

    /** Solicitud desde la página pública. "sitioWeb" es un campo trampa anti-spam: debe venir vacío. */
    public record CitaPublicaRequest(
            @NotBlank(message = "Tu nombre es obligatorio") @Size(max = 120) String nombre,
            @NotBlank(message = "Tu teléfono es obligatorio") @Pattern(regexp = "^[0-9+()\\- ]{6,20}$", message = "Teléfono inválido") String telefono,
            @Email(message = "Correo inválido") @Size(max = 100) String correo,
            @NotBlank(message = "La placa es obligatoria") @Size(max = 10) String placa,
            @Size(max = 40) String marca,
            @Size(max = 40) String modelo,
            @NotNull(message = "Elige el servicio") Long servicioId,
            @NotNull(message = "Elige fecha y hora") LocalDateTime fechaHora,
            @Size(max = 300) String motivo,
            @Size(max = 100) String sitioWeb) {
    }

    public record CitaPublicaResponse(String codigo, LocalDateTime fechaHora, String mensaje) {
    }

    public record FranjaDto(String hora, int libres) {
    }

    public record DisponibilidadDto(LocalDate fecha, boolean laborable, List<FranjaDto> franjas) {
    }
}
