package pe.mecauto.service.reporte;

import pe.mecauto.dto.ReporteDto;

/**
 * Patrón Strategy: cada tipo de reporte es una estrategia intercambiable con la misma interfaz.
 * Para agregar un reporte nuevo basta con crear otra clase @Component que la implemente (principio Abierto/Cerrado).
 */
public interface ReporteGenerador {

    /** Identificador usado en la URL: /reportes/{tipo}. */
    String tipo();

    /** Debe llamarse con un filtro ya normalizado. */
    ReporteDto generar(FiltroReporte filtro);
}
