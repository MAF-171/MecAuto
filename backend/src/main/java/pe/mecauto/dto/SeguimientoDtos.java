package pe.mecauto.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import pe.mecauto.domain.EstadoOrden;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** DTO de la consulta pública del estado de una orden. Solo contiene datos mínimos, nunca datos personales. */
public final class SeguimientoDtos {

    private SeguimientoDtos() {
    }

    /** Código de la orden (ej. OT-0002) y los últimos 3 caracteres de la placa como segunda comprobación. */
    public record ConsultaOrdenRequest(
            @NotBlank(message = "Escribe el código de tu orden") @Size(max = 12) String codigo,
            @NotBlank(message = "Escribe los últimos 3 caracteres de la placa") @Size(min = 3, max = 10, message = "Escribe al menos 3 caracteres de la placa") String placa) {
    }

    /** Estado público de la orden. La placa va enmascarada (ej. ***-029). */
    public record SeguimientoOrdenDto(String codigo, EstadoOrden estado, String placa, String servicio,
                                      LocalDateTime fechaIngreso, LocalDate fechaEntregaEstimada) {
    }
}
