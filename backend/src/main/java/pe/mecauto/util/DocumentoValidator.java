package pe.mecauto.util;

import pe.mecauto.domain.TipoDocumento;

import java.util.regex.Pattern;

/** Validación de documentos de identidad peruanos (formato, no consulta a RENIEC/SUNAT). */
public final class DocumentoValidator {

    private static final Pattern DNI = Pattern.compile("^\\d{8}$");
    private static final Pattern RUC = Pattern.compile("^(10|15|17|20)\\d{9}$");
    private static final Pattern CE = Pattern.compile("^[A-Za-z0-9]{9,12}$");

    private DocumentoValidator() {
    }

    public static boolean esValido(TipoDocumento tipo, String numero) {
        if (tipo == null || numero == null) {
            return false;
        }
        String n = numero.trim();
        return switch (tipo) {
            case DNI -> DNI.matcher(n).matches();
            case RUC -> RUC.matcher(n).matches();
            case CE -> CE.matcher(n).matches();
        };
    }

    public static String mensajeFormato(TipoDocumento tipo) {
        return switch (tipo) {
            case DNI -> "El DNI debe tener 8 dígitos";
            case RUC -> "El RUC debe tener 11 dígitos y empezar con 10, 15, 17 o 20";
            case CE -> "El carné de extranjería debe tener entre 9 y 12 caracteres alfanuméricos";
        };
    }
}
