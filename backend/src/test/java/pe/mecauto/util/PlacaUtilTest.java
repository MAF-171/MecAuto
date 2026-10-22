package pe.mecauto.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlacaUtilTest {

    @Test
    @DisplayName("Normaliza a mayúsculas y sin espacios")
    void normaliza() {
        assertEquals("ABC-123", PlacaUtil.normalizar("  abc -123 "));
        assertNull(PlacaUtil.normalizar(null));
    }

    @Test
    @DisplayName("Quita cualquier tipo de espacio y no cambia una placa ya normalizada")
    void quitaEspacios() {
        assertEquals("ABC123", PlacaUtil.normalizar("a b\tc 1 2 3"));
        assertEquals("ABC-123", PlacaUtil.normalizar("ABC-123"));
        assertEquals("", PlacaUtil.normalizar("   "));
    }

    @Test
    @DisplayName("Acepta formatos de placa comunes")
    void validas() {
        assertTrue(PlacaUtil.esValida("ABC-123"));
        assertTrue(PlacaUtil.esValida("ABC123"));
        assertTrue(PlacaUtil.esValida("A1B-1234"));
    }

    @Test
    @DisplayName("Rechaza placas mal formadas")
    void invalidas() {
        assertFalse(PlacaUtil.esValida(""));
        assertFalse(PlacaUtil.esValida("AB"));
        assertFalse(PlacaUtil.esValida("ABC-12-3"));
        assertFalse(PlacaUtil.esValida("ABC_123"));
        assertFalse(PlacaUtil.esValida(null));
    }
}
