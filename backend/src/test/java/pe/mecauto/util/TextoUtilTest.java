package pe.mecauto.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TextoUtilTest {

    @Test
    @DisplayName("limpiarBusqueda: null y vacío dan cadena vacía; recorta espacios")
    void limpiarBusqueda() {
        assertEquals("", TextoUtil.limpiarBusqueda(null));
        assertEquals("", TextoUtil.limpiarBusqueda("   "));
        assertEquals("filtro", TextoUtil.limpiarBusqueda("  filtro "));
    }

    @Test
    @DisplayName("aNullSiVacio: vacío o solo espacios se vuelve null")
    void aNullSiVacio() {
        assertNull(TextoUtil.aNullSiVacio(null));
        assertNull(TextoUtil.aNullSiVacio(""));
        assertNull(TextoUtil.aNullSiVacio("   "));
        assertEquals("juan@correo.pe", TextoUtil.aNullSiVacio(" juan@correo.pe "));
    }

    @Test
    @DisplayName("normalizarEspacios: reduce espacios repetidos en nombres")
    void normalizarEspacios() {
        assertEquals("Juan Pérez Soto", TextoUtil.normalizarEspacios("  Juan   Pérez \t Soto  "));
        assertNull(TextoUtil.normalizarEspacios(null));
    }

    @Test
    @DisplayName("escaparLike: escapa % y _ para buscar texto literal")
    void escaparLike() {
        assertEquals("50\\% desc\\_ok", TextoUtil.escaparLike("50% desc_ok"));
    }
}
