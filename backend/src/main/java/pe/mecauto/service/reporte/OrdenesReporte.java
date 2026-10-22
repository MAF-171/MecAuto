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

@Component
@Transactional(readOnly = true)
public class OrdenesReporte implements ReporteGenerador {

    private final OrdenTrabajoRepository ordenes;

    public OrdenesReporte(OrdenTrabajoRepository ordenes) {
        this.ordenes = ordenes;
    }

    @Override
    public String tipo() {
        return "ordenes";
    }

    @Override
    public ReporteDto generar(FiltroReporte f) {
        Specification<OrdenTrabajo> spec = (r, cq, cb) -> cb.and(
                cb.greaterThanOrEqualTo(r.<LocalDateTime>get("fechaIngreso"), f.inicio()),
                cb.lessThan(r.<LocalDateTime>get("fechaIngreso"), f.finExclusivo()));
        if (f.estado() != null) {
            EstadoOrden e = EstadoOrden.valueOf(f.estado());
            spec = spec.and((r, cq, cb) -> cb.equal(r.get("estado"), e));
        }
        List<OrdenTrabajo> lista = ordenes.findAll(spec, Sort.by("fechaIngreso"));
        List<List<Object>> filas = new ArrayList<>();
        Map<String, Object> resumen = new LinkedHashMap<>();
        Map<EstadoOrden, Long> porEstado = new java.util.EnumMap<>(EstadoOrden.class);
        BigDecimal total = BigDecimal.ZERO;
        for (OrdenTrabajo o : lista) {
            filas.add(List.of(o.getCodigo(), Formato.fechaHora(o.getFechaIngreso()), o.getCliente().getNombre(),
                    o.getVehiculo().getPlaca(), o.getMecanico() == null ? "Sin asignar" : o.getMecanico().getNombre(),
                    o.getEstado().name(), o.getTotal()));
            porEstado.merge(o.getEstado(), 1L, Long::sum);
            if (o.getEstado() != EstadoOrden.CANCELADA) {
                total = total.add(o.getTotal());
            }
        }
        resumen.put("Órdenes", (long) lista.size());
        porEstado.forEach((k, v) -> resumen.put(k.name(), v));
        resumen.put("Monto total sin canceladas (S/)", total);
        return new ReporteDto(tipo(), "Órdenes de trabajo", f.periodo(),
                List.of("Código", "Ingreso", "Cliente", "Placa", "Mecánico", "Estado", "Total (S/)"), filas, resumen);
    }
}
