package pe.mecauto.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.mecauto.domain.TipoDocumento;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentoValidatorTest {

    @Test
    @DisplayName("DNI: exactamente 8 dígitos")
    void dni() {
        assertTrue(DocumentoValidator.esValido(TipoDocumento.DNI, "12345678"));
        assertFalse(DocumentoValidator.esValido(TipoDocumento.DNI, "1234567"));
        assertFalse(DocumentoValidator.esValido(TipoDocumento.DNI, "123456789"));
        assertFalse(DocumentoValidator.esValido(TipoDocumento.DNI, "1234567a"));
    }

    @Test
    @DisplayName("RUC: 11 dígitos que empiezan con 10, 15, 17 o 20")
    void ruc() {
        assertTrue(DocumentoValidator.esValido(TipoDocumento.RUC, "20123456789"));
        assertTrue(DocumentoValidator.esValido(TipoDocumento.RUC, "10123456789"));
        assertFalse(DocumentoValidator.esValido(TipoDocumento.RUC, "30123456789"));
        assertFalse(DocumentoValidator.esValido(TipoDocumento.RUC, "2012345678"));
    }

    @Test
    @DisplayName("Carné de extranjería: 9 a 12 alfanuméricos")
    void ce() {
        assertTrue(DocumentoValidator.esValido(TipoDocumento.CE, "001234567"));
        assertFalse(DocumentoValidator.esValido(TipoDocumento.CE, "1234"));
        assertFalse(DocumentoValidator.esValido(TipoDocumento.CE, "12345-6789"));
    }

    @Test
    @DisplayName("Nulos no son válidos")
    void nulos() {
        assertFalse(DocumentoValidator.esValido(null, "12345678"));
        assertFalse(DocumentoValidator.esValido(TipoDocumento.DNI, null));
    }
}
