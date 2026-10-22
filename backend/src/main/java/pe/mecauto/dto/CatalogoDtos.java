package pe.mecauto.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import pe.mecauto.domain.TipoMovimiento;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Servicios, proveedores, repuestos e inventario. */
public final class CatalogoDtos {

    private CatalogoDtos() {
    }

    public record ServicioDto(Long id, String nombre, String descripcion, BigDecimal precioReferencial,
                              Integer duracionMinutos, boolean activo) {
    }

    public record ServicioRequest(
            @NotBlank(message = "El nombre es obligatorio") @Size(max = 100) String nombre,
            @Size(max = 500) String descripcion,
            @DecimalMin(value = "0.00", message = "El precio no puede ser negativo") @Digits(integer = 10, fraction = 2) BigDecimal precioReferencial,
            @Min(value = 1, message = "La duración debe ser de al menos 1 minuto") Integer duracionMinutos) {
    }

    public record ProveedorDto(Long id, String razonSocial, String ruc, String telefono, boolean activo) {
    }

    public record ProveedorRequest(
            @NotBlank(message = "La razón social es obligatoria") @Size(max = 120) String razonSocial,
            @NotBlank(message = "El RUC es obligatorio") @Pattern(regexp = "^(10|15|17|20)\\d{9}$", message = "RUC inválido (11 dígitos)") String ruc,
            @Pattern(regexp = "^$|^[0-9+()\\- ]{6,20}$", message = "Teléfono inválido") String telefono) {
    }

    public record RepuestoDto(Long id, Long proveedorId, String proveedorNombre, String codigo, String nombre,
                              String categoria, BigDecimal precioCompra, BigDecimal precioVenta,
                              int stockActual, int stockMinimo, boolean bajoStock, boolean activo) {
    }

    public record RepuestoRequest(
            Long proveedorId,
            @NotBlank(message = "El código es obligatorio") @Size(max = 20) String codigo,
            @NotBlank(message = "El nombre es obligatorio") @Size(max = 100) String nombre,
            @Size(max = 40) String categoria,
            @NotNull(message = "El precio de compra es obligatorio") @DecimalMin(value = "0.00", message = "No puede ser negativo") @Digits(integer = 10, fraction = 2) BigDecimal precioCompra,
            @NotNull(message = "El precio de venta es obligatorio") @DecimalMin(value = "0.00", message = "No puede ser negativo") @Digits(integer = 10, fraction = 2) BigDecimal precioVenta,
            @NotNull(message = "El stock mínimo es obligatorio") @Min(value = 0, message = "No puede ser negativo") Integer stockMinimo,
            @Min(value = 0, message = "No puede ser negativo") Integer stockInicial) {
    }

    /**
     * ENTRADA y SALIDA: cantidad = unidades (mayor que 0).
     * AJUSTE: cantidad = stock real contado (0 o más); el motivo es obligatorio.
     * claveIdempotencia: texto único (por ejemplo un UUID) que genera el frontend; si se reenvía la misma
     * solicitud no se registra dos veces.
     */
    public record MovimientoRequest(
            @NotNull(message = "Elige el tipo de movimiento") TipoMovimiento tipo,
            @NotNull(message = "La cantidad es obligatoria") @Min(value = 0, message = "La cantidad no puede ser negativa") Integer cantidad,
            @Size(max = 200) String motivo,
            @Size(max = 80) String claveIdempotencia) {
    }

    public record MovimientoDto(Long id, Long repuestoId, String repuestoCodigo, String repuestoNombre,
                                TipoMovimiento tipo, int cantidad, int stockResultante, String motivo,
                                Long ordenId, String usuarioNombre, LocalDateTime fecha) {
    }

    /** Información pública del servicio (sin datos internos). */
    public record ServicioPublicoDto(Long id, String nombre, String descripcion, BigDecimal precioReferencial,
                                     Integer duracionMinutos) {
    }
}
