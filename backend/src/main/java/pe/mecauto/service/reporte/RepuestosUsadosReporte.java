package pe.mecauto.service.reporte;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pe.mecauto.domain.EstadoOrden;
import pe.mecauto.dto.ReporteDto;
import pe.mecauto.repository.DetalleOrdenRepuestoRepository;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Repuestos consumidos en órdenes completadas o entregadas del periodo. */
@Component
@Transactional(readOnly = true)
public class RepuestosUsadosReporte implements ReporteGenerador {

    private final DetalleOrdenRepuestoRepository detalles;

    public RepuestosUsadosReporte(DetalleOrdenRepuestoRepository detalles) {
        this.detalles = detalles;
    }

    @Override
    public String tipo() {
        return "repuestos-usados";
    }

    @Override
    public ReporteDto generar(FiltroReporte f) {
        List<List<Object>> filas = new ArrayList<>();
        long total = 0;
        for (Object[] fila : detalles.repuestosMasUsados(
                EnumSet.of(EstadoOrden.COMPLETADA, EstadoOrden.ENTREGADA), f.inicio(), f.finExclusivo())) {
            long cantidad = ((Number) fila[2]).longValue();
            filas.add(List.of((String) fila[0], (String) fila[1], cantidad));
            total += cantidad;
        }
        Map<String, Object> resumen = new LinkedHashMap<>();
        resumen.put("Repuestos distintos", (long) filas.size());
        resumen.put("Unidades usadas", total);
        return new ReporteDto(tipo(), "Repuestos utilizados", f.periodo(), List.of("Código", "Repuesto", "Cantidad"), filas, resumen);
    }
}
