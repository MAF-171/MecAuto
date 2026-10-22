package pe.mecauto.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.mecauto.domain.EstadoCotizacion;
import pe.mecauto.domain.TipoLineaCotizacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class CotizacionDtos {

    private CotizacionDtos() {
    }

    public record LineaCotizacionDto(Long id, TipoLineaCotizacion tipo, Long servicioId, Long repuestoId,
                                     String descripcion, int cantidad, BigDecimal precioUnitario, BigDecimal importe) {
    }

    /** detalles viene vacío (null) en los listados y completo en la consulta individual. */
    public record CotizacionDto(Long id, String codigo, Long clienteId, String clienteNombre, Long vehiculoId,
                                String placa, Long ordenId, LocalDate fechaEmision, LocalDate vigenciaHasta,
                                EstadoCotizacion estado, boolean vencida, BigDecimal subtotal, BigDecimal descuento,
                                BigDecimal baseImponible, BigDecimal igv, BigDecimal total, String observaciones,
                                List<LineaCotizacionDto> detalles) {
    }

    public record LineaCotizacionRequest(
            @NotNull(message = "Indica el tipo de línea") TipoLineaCotizacion tipo,
            Long servicioId,
            Long repuestoId,
            @Size(max = 200) String descripcion,
            @NotNull(message = "La cantidad es obligatoria") @Min(value = 1, message = "La cantidad mínima es 1") Integer cantidad,
            @DecimalMin(value = "0.00", message = "No puede ser negativo") @Digits(integer = 10, fraction = 2) BigDecimal precioUnitario) {
    }

    public record CotizacionRequest(
            @NotNull(message = "Elige el cliente") Long clienteId,
            Long vehiculoId,
            LocalDate vigenciaHasta,
            @DecimalMin(value = "0.00", message = "El descuento no puede ser negativo") @Digits(integer = 10, fraction = 2) BigDecimal descuento,
            @Size(max = 500) String observaciones,
            @NotEmpty(message = "Agrega al menos una línea") @Valid List<LineaCotizacionRequest> lineas) {
    }
}
