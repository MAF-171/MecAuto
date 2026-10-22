package pe.mecauto.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.mecauto.domain.CalculadoraIgv;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Cálculo de una cotización con dos líneas y descuento: el IGV se extrae del total
 * (los precios ya lo incluyen) y nunca se suma por encima.
 */
class CotizacionConIgvTest {

    @Test
    @DisplayName("2 servicios x 80 + 3 repuestos x 28 - descuento 10 = 234.00 con IGV incluido")
    void totalConDescuento() {
        BigDecimal servicios = CalculadoraIgv.importe(2, new BigDecimal("80.00"));
        BigDecimal repuestos = CalculadoraIgv.importe(3, new BigDecimal("28.00"));
        BigDecimal total = servicios.add(repuestos).subtract(new BigDecimal("10.00"));

        var d = CalculadoraIgv.desglosar(total);

        assertEquals(new BigDecimal("234.00"), d.total());
        assertEquals(new BigDecimal("198.31"), d.baseImponible());
        assertEquals(new BigDecimal("35.69"), d.igv());
        assertEquals(d.total(), d.baseImponible().add(d.igv()));
    }
}
