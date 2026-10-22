package pe.mecauto.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import pe.mecauto.domain.TipoDocumento;

import java.time.LocalDateTime;

/** Clientes y vehículos. */
public final class PersonaDtos {

    private PersonaDtos() {
    }

    public record ClienteDto(Long id, TipoDocumento tipoDocumento, String nroDocumento, String nombre,
                             String telefono, String correo, String direccion, boolean activo,
                             LocalDateTime creadoEn, Long cantidadVehiculos) {
    }

    public record ClienteRequest(
            @NotNull(message = "Elige el tipo de documento") TipoDocumento tipoDocumento,
            @NotBlank(message = "El número de documento es obligatorio") @Size(max = 12) String nroDocumento,
            @NotBlank(message = "El nombre o razón social es obligatorio") @Size(max = 120) String nombre,
            @Pattern(regexp = "^$|^[0-9+()\\- ]{6,20}$", message = "Teléfono inválido") String telefono,
            @Email(message = "Correo inválido") @Size(max = 100) String correo,
            @Size(max = 200) String direccion) {
    }

    public record VehiculoDto(Long id, Long clienteId, String clienteNombre, String placa, String marca,
                              String modelo, Integer anio, String color, int kilometraje, boolean activo) {
    }

    public record VehiculoRequest(
            @NotNull(message = "Elige el propietario") Long clienteId,
            @NotBlank(message = "La placa es obligatoria") @Size(max = 10) String placa,
            @NotBlank(message = "La marca es obligatoria") @Size(max = 40) String marca,
            @NotBlank(message = "El modelo es obligatorio") @Size(max = 40) String modelo,
            @Min(value = 1950, message = "Año inválido") @Max(value = 2100, message = "Año inválido") Integer anio,
            @Size(max = 30) String color,
            @NotNull(message = "El kilometraje es obligatorio") @Min(value = 0, message = "El kilometraje no puede ser negativo") Integer kilometraje) {
    }

    /** Una fila del historial de mantenimiento de un vehículo (se deriva de sus órdenes de trabajo). */
    public record HistorialMantenimientoDto(Long ordenId, String codigo, java.time.LocalDateTime fechaIngreso,
                                            java.time.LocalDateTime fechaCierre, Integer kilometraje,
                                            String falla, String diagnostico, String trabajosRealizados,
                                            pe.mecauto.domain.EstadoOrden estado, String mecanico,
                                            java.math.BigDecimal total) {
    }
}
