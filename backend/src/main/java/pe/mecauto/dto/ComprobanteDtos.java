package pe.mecauto.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.mecauto.domain.EstadoComprobante;
import pe.mecauto.domain.MetodoPago;
import pe.mecauto.domain.TipoComprobante;
import pe.mecauto.domain.TipoDocumento;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class ComprobanteDtos {

    private ComprobanteDtos() {
    }

    public record LineaComprobanteDto(String descripcion, int cantidad, BigDecimal precioUnitario, BigDecimal importe) {
    }

    /** taller: datos del emisor (nombre, ruc, direccion, telefono) para la vista imprimible. */
    public record ComprobanteDto(Long id, Long ordenId, String ordenCodigo, TipoComprobante tipo, String serie,
                                 String numero, String numeroCompleto, String clienteNombre, TipoDocumento clienteTipoDoc,
                                 String clienteDocumento, String vehiculoPlaca, BigDecimal baseImponible, BigDecimal igv,
                                 BigDecimal total, MetodoPago metodoPago, EstadoComprobante estado,
                                 String motivoAnulacion, LocalDateTime fechaEmision, String emitidoPor,
                                 List<LineaComprobanteDto> detalles, Map<String, String> taller) {
    }

    public record EmitirComprobanteRequest(
            @NotNull(message = "Elige la orden") Long ordenId,
            @NotNull(message = "Elige el método de pago") MetodoPago metodoPago) {
    }

    public record AnularComprobanteRequest(
            @NotBlank(message = "Indica el motivo de la anulación") @Size(max = 200) String motivo) {
    }
}
