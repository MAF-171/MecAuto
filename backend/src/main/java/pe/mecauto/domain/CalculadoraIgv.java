package pe.mecauto.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Cálculo del IGV (18 %). Regla RN06: los precios YA INCLUYEN IGV.
 * Por eso el IGV nunca se suma otra vez: se "desglosa" del total.
 *
 *   base = total / 1.18      igv = total - base
 * Ejemplo del documento: total 216.00 -> base 183.05, IGV 32.95.
 */
public final class CalculadoraIgv {

    public static final BigDecimal TASA = new BigDecimal("0.18");
    private static final BigDecimal FACTOR = BigDecimal.ONE.add(TASA); // 1.18

    private CalculadoraIgv() {
    }

    /** Resultado inmutable del desglose. */
    public record Desglose(BigDecimal baseImponible, BigDecimal igv, BigDecimal total) {
    }

    public static Desglose desglosar(BigDecimal totalConIgv) {
        BigDecimal total = redondear(totalConIgv);
        BigDecimal base = total.divide(FACTOR, 2, RoundingMode.HALF_UP);
        BigDecimal igv = total.subtract(base);
        return new Desglose(base, igv, total);
    }

    /** Importe de una línea = cantidad x precio unitario (con IGV incluido). */
    public static BigDecimal importe(int cantidad, BigDecimal precioUnitario) {
        return redondear(precioUnitario.multiply(BigDecimal.valueOf(cantidad)));
    }

    public static BigDecimal redondear(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }
}
