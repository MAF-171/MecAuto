package pe.mecauto.dto;

import java.util.List;
import java.util.Map;

/** Resultado genérico de un reporte: la misma estructura sirve para la pantalla, la impresión y el Excel. */
public record ReporteDto(String tipo, String titulo, String periodo, List<String> columnas,
                         List<List<Object>> filas, Map<String, Object> resumen) {
}
