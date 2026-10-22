package pe.mecauto.util;

import org.apache.commons.lang3.StringUtils;

/** Utilidades pequeñas de texto para búsquedas y comparación (apoyadas en Apache Commons Lang 3). */
public final class TextoUtil {

    private TextoUtil() {
    }

    /** Devuelve el texto sin espacios sobrantes; null o vacío se vuelve "" (así las búsquedas LIKE no reciben null). */
    public static String limpiarBusqueda(String q) {
        return StringUtils.trimToEmpty(q);
    }

    /** Texto sin espacios sobrantes, o null si queda vacío. */
    public static String aNullSiVacio(String s) {
        return StringUtils.trimToNull(s);
    }

    /** Quita espacios al inicio y al final y reduce los espacios repetidos del medio a uno solo (nombres de personas). */
    public static String normalizarEspacios(String s) {
        return StringUtils.normalizeSpace(s);
    }

    /** Escapa los comodines de LIKE (%, _) para buscar texto literal. */
    public static String escaparLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
