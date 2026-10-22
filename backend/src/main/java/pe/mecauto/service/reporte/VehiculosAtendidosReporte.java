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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Vehículos con al menos una orden completada o entregada en el periodo. */
@Component
@Transactional(readOnly = true)
public class VehiculosAtendidosReporte implements ReporteGenerador {

    private final OrdenTrabajoRepository ordenes;

    public VehiculosAtendidosReporte(OrdenTrabajoRepository ordenes) {
        this.ordenes = ordenes;
    }

    @Override
    public String tipo() {
        return "vehiculos-atendidos";
    }

    @Override
    public ReporteDto generar(FiltroReporte f) {
        Specification<OrdenTrabajo> spec = (r, cq, cb) -> cb.and(
                cb.greaterThanOrEqualTo(r.<LocalDateTime>get("fechaIngreso"), f.inicio()),
                cb.lessThan(r.<LocalDateTime>get("fechaIngreso"), f.finExclusivo()),
                r.get("estado").in(EstadoOrden.COMPLETADA, EstadoOrden.ENTREGADA));
        List<OrdenTrabajo> lista = ordenes.findAll(spec, Sort.by("fechaIngreso"));

        Map<Long, Object[]> acumulado = new LinkedHashMap<>(); // idVehiculo -> [orden más reciente, cantidad, total]
        for (OrdenTrabajo o : lista) {
            Object[] a = acumulado.computeIfAbsent(o.getVehiculo().getId(), k -> new Object[]{o, 0L, BigDecimal.ZERO});
            a[0] = o;
            a[1] = (Long) a[1] + 1;
            a[2] = ((BigDecimal) a[2]).add(o.getTotal());
        }
        List<List<Object>> filas = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (Object[] a : acumulado.values()) {
            OrdenTrabajo o = (OrdenTrabajo) a[0];
            filas.add(List.of(o.getVehiculo().getPlaca(), o.getVehiculo().getMarca() + " " + o.getVehiculo().getModelo(),
                    o.getCliente().getNombre(), (Long) a[1], Formato.fechaHora(o.getFechaIngreso()), (BigDecimal) a[2]));
            total = total.add((BigDecimal) a[2]);
        }
        Map<String, Object> resumen = new LinkedHashMap<>();
        resumen.put("Vehículos atendidos", (long) filas.size());
        resumen.put("Órdenes", (long) lista.size());
        resumen.put("Monto total (S/)", total);
        return new ReporteDto(tipo(), "Vehículos atendidos", f.periodo(),
                List.of("Placa", "Vehículo", "Cliente", "Órdenes", "Último ingreso", "Total (S/)"), filas, resumen);
    }
}
