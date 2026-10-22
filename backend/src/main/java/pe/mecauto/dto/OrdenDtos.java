package pe.mecauto.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.mecauto.domain.EstadoOrden;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class OrdenDtos {

    private OrdenDtos() {
    }

    public record OrdenResumenDto(Long id, String codigo, Long clienteId, String clienteNombre, Long vehiculoId,
                                  String placa, String vehiculo, Long mecanicoId, String mecanicoNombre,
                                  String falla, EstadoOrden estado, LocalDateTime fechaIngreso,
                                  LocalDate fechaEntregaEstimada, BigDecimal total) {
    }

    public record LineaServicioDto(Long id, Long servicioId, String descripcion, int cantidad,
                                   BigDecimal precioUnitario, BigDecimal importe) {
    }

    public record LineaRepuestoDto(Long id, Long repuestoId, String codigo, String descripcion, int cantidad,
                                   BigDecimal precioUnitario, BigDecimal importe, boolean descontado,
                                   int stockDisponible) {
    }

    public record HistorialEstadoDto(EstadoOrden estadoAnterior, EstadoOrden estadoNuevo, String usuario,
                                     String comentario, LocalDateTime fecha) {
    }

    public record OrdenDetalleDto(Long id, String codigo, Long clienteId, String clienteNombre, String clienteDocumento,
                                  Long vehiculoId, String placa, String vehiculo, Long mecanicoId, String mecanicoNombre,
                                  Long citaId, String falla, String diagnostico, String trabajosRealizados,
                                  Integer kilometrajeIngreso, EstadoOrden estado, LocalDateTime fechaIngreso,
                                  LocalDate fechaEntregaEstimada, LocalDateTime fechaCierre, BigDecimal manoObra,
                                  BigDecimal descuento, BigDecimal total, BigDecimal baseImponible, BigDecimal igv,
                                  List<LineaServicioDto> servicios, List<LineaRepuestoDto> repuestos,
                                  List<HistorialEstadoDto> historial, Long comprobanteId, String comprobanteNumero,
                                  List<EstadoOrden> transicionesPermitidas, boolean editable) {
    }

    public record OrdenCrearRequest(
            @NotNull(message = "Elige el vehículo") Long vehiculoId,
            @NotBlank(message = "Describe la falla o el servicio solicitado") @Size(max = 500) String falla,
            Long mecanicoId,
            @Min(value = 0, message = "El kilometraje no puede ser negativo") Integer kilometrajeIngreso,
            LocalDate fechaEntregaEstimada,
            Long citaId) {
    }

    public record OrdenActualizarRequest(
            @NotBlank(message = "La falla es obligatoria") @Size(max = 500) String falla,
            @Size(max = 1000) String diagnostico,
            @Size(max = 1000) String trabajosRealizados,
            @DecimalMin(value = "0.00", message = "El descuento no puede ser negativo") @Digits(integer = 10, fraction = 2) BigDecimal descuento,
            @NotNull(message = "La mano de obra es obligatoria (puede ser 0)") @DecimalMin(value = "0.00", message = "No puede ser negativa") @Digits(integer = 10, fraction = 2) BigDecimal manoObra,
            LocalDate fechaEntregaEstimada,
            @Min(value = 0, message = "El kilometraje no puede ser negativo") Integer kilometrajeIngreso) {
    }

    public record AsignarMecanicoRequest(Long mecanicoId) {
    }

    public record CambioEstadoRequest(
            @NotNull(message = "Elige el nuevo estado") EstadoOrden estado,
            @Size(max = 300) String comentario) {
    }

    public record AgregarServicioRequest(
            @NotNull(message = "Elige el servicio") Long servicioId,
            @NotNull(message = "La cantidad es obligatoria") @Min(value = 1, message = "La cantidad mínima es 1") Integer cantidad,
            @DecimalMin(value = "0.00", message = "No puede ser negativo") @Digits(integer = 10, fraction = 2) BigDecimal precioUnitario,
            @Size(max = 200) String descripcion) {
    }

    public record AgregarRepuestoRequest(
            @NotNull(message = "Elige el repuesto") Long repuestoId,
            @NotNull(message = "La cantidad es obligatoria") @Min(value = 1, message = "La cantidad mínima es 1") Integer cantidad) {
    }
}
