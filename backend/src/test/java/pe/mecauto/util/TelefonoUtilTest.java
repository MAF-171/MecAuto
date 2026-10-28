package pe.mecauto.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TelefonoUtilTest {

    @Test
    @DisplayName("Celular peruano escrito de distintas formas queda igual")
    void normalizaCelularPeruano() {
        assertEquals("+51987654321", TelefonoUtil.normalizar("987654321"));
        assertEquals("+51987654321", TelefonoUtil.normalizar("987 654 321"));
        assertEquals("+51987654321", TelefonoUtil.normalizar("+51 987-654-321"));
        assertEquals("+51987654321", TelefonoUtil.normalizar("51987654321"));
    }

    @Test
    @DisplayName("Textos que no son un número de celular devuelven null")
    void rechazaInvalidos() {
        assertNull(TelefonoUtil.normalizar(null));
        assertNull(TelefonoUtil.normalizar("abc"));
        assertNull(TelefonoUtil.normalizar("12345"));
        assertNull(TelefonoUtil.normalizar("1234567890123456"));
    }
}
