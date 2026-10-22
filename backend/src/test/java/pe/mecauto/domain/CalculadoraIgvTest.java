package pe.mecauto.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.mecauto.domain.CalculadoraIgv.Desglose;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CalculadoraIgvTest {

    @Test
    @DisplayName("El precio ya incluye IGV: 216.00 -> base 183.05 + IGV 32.95")
    void desglosaTotalConIgv() {
        Desglose d = CalculadoraIgv.desglosar(new BigDecimal("216.00"));
        assertEquals(new BigDecimal("183.05"), d.baseImponible());
        assertEquals(new BigDecimal("32.95"), d.igv());
        assertEquals(new BigDecimal("216.00"), d.total());
    }

    @Test
    @DisplayName("236.00 -> base 200.00 + IGV 36.00 (exacto)")
    void desgloseExacto() {
        Desglose d = CalculadoraIgv.desglosar(new BigDecimal("236"));
        assertEquals(new BigDecimal("200.00"), d.baseImponible());
        assertEquals(new BigDecimal("36.00"), d.igv());
    }

    @Test
    @DisplayName("El IGV no se suma dos veces: base + IGV siempre es igual al total")
    void baseMasIgvIgualTotal() {
        for (String s : new String[]{"0.01", "1.00", "19.99", "120.00", "999.99", "1234.56"}) {
            Desglose d = CalculadoraIgv.desglosar(new BigDecimal(s));
            assertEquals(d.total(), d.baseImponible().add(d.igv()), "total " + s);
        }
    }

    @Test
    @DisplayName("Importe de línea = cantidad x precio, redondeado a 2 decimales")
    void importeDeLinea() {
        assertEquals(new BigDecimal("84.00"), CalculadoraIgv.importe(3, new BigDecimal("28")));
        assertEquals(new BigDecimal("0.03"), CalculadoraIgv.importe(3, new BigDecimal("0.01")));
    }

    @Test
    @DisplayName("Redondeo HALF_UP")
    void redondeo() {
        assertEquals(new BigDecimal("1.01"), CalculadoraIgv.redondear(new BigDecimal("1.005")));
        assertEquals(new BigDecimal("1.00"), CalculadoraIgv.redondear(new BigDecimal("1.004")));
    }
}
