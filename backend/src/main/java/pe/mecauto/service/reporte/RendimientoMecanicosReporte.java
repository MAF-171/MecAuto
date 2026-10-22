package pe.mecauto.service.reporte;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pe.mecauto.domain.EstadoOrden;
import pe.mecauto.dto.ReporteDto;
import pe.mecauto.entity.OrdenTrabajo;
import pe.mecauto.repository.OrdenTrabajoRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Rendimiento operativo real por mecánico: órdenes asignadas en el periodo, cuántas se completaron
 * y el tiempo promedio (ingreso -> cierre) solo de las que tienen fecha de cierre registrada.
 */
@Component
@Transactional(readOnly = true)
public class RendimientoMecanicosReporte implements ReporteGenerador {

    private final OrdenTrabajoRepository ordenes;

    public RendimientoMecanicosReporte(OrdenTrabajoRepository ordenes) {
        this.ordenes = ordenes;
    }

    @Override
    public String tipo() {
        return "rendimiento-mecanicos";
    }

    @Override
    public ReporteDto generar(FiltroReporte f) {
        Specification<OrdenTrabajo> spec = (r, cq, cb) -> cb.and(
                cb.greaterThanOrEqualTo(r.<LocalDateTime>get("fechaIngreso"), f.inicio()),
                cb.lessThan(r.<LocalDateTime>get("fechaIngreso"), f.finExclusivo()),
                cb.isNotNull(r.get("mecanico")));
        Map<String, long[]> acumulado = new LinkedHashMap<>(); // nombre -> [asignadas, completadas, canceladas, cierres, minutosTotales]
        for (OrdenTrabajo o : ordenes.findAll(spec, Sort.by("fechaIngreso"))) {
            long[] a = acumulado.computeIfAbsent(o.getMecanico().getNombre(), k -> new long[5]);
            a[0]++;
            if (o.getEstado() == EstadoOrden.COMPLETADA || o.getEstado() == EstadoOrden.ENTREGADA) {
                a[1]++;
            }
            if (o.getEstado() == EstadoOrden.CANCELADA) {
                a[2]++;
            }
            if (o.getFechaCierre() != null) {
                a[3]++;
                a[4] += Duration.between(o.getFechaIngreso(), o.getFechaCierre()).toMinutes();
            }
        }
        List<List<Object>> filas = new ArrayList<>();
        for (Map.Entry<String, long[]> e : acumulado.entrySet()) {
            long[] a = e.getValue();
            Object promedio = a[3] == 0 ? "—"
                    : BigDecimal.valueOf(a[4]).divide(BigDecimal.valueOf(a[3] * 60), 1, RoundingMode.HALF_UP);
            filas.add(List.of(e.getKey(), a[0], a[1], a[2], promedio));
        }
        Map<String, Object> resumen = new LinkedHashMap<>();
        resumen.put("Mecánicos con órdenes", (long) filas.size());
        return new ReporteDto(tipo(), "Rendimiento por mecánico", f.periodo(),
                List.of("Mecánico", "Órdenes asignadas", "Completadas/Entregadas", "Canceladas", "Horas promedio hasta cierre"), filas, resumen);
    }
}
