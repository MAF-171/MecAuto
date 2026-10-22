package pe.mecauto.util;

import org.apache.commons.lang3.StringUtils;

import java.util.Locale;
import java.util.regex.Pattern;

/** Normaliza y valida placas: mayúsculas y sin espacios (ej. " abc -123 " -> "ABC-123"). Usa Apache Commons Lang 3. */
public final class PlacaUtil {

    private static final Pattern FORMATO = Pattern.compile("^[A-Z0-9]{3}-?[A-Z0-9]{3,4}$");

    private PlacaUtil() {
    }

    public static String normalizar(String placa) {
        if (placa == null) {
            return null;
        }
        return StringUtils.deleteWhitespace(StringUtils.upperCase(placa, Locale.ROOT));
    }

    public static boolean esValida(String placaNormalizada) {
        return placaNormalizada != null && FORMATO.matcher(placaNormalizada).matches();
    }
}
