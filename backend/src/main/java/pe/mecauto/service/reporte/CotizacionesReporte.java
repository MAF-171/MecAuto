package pe.mecauto.service.reporte;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pe.mecauto.domain.EstadoCotizacion;
import pe.mecauto.dto.ReporteDto;
import pe.mecauto.entity.Cotizacion;
import pe.mecauto.repository.CotizacionRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@Transactional(readOnly = true)
public class CotizacionesReporte implements ReporteGenerador {

    private final CotizacionRepository cotizaciones;

    public CotizacionesReporte(CotizacionRepository cotizaciones) {
        this.cotizaciones = cotizaciones;
    }

    @Override
    public String tipo() {
        return "cotizaciones";
    }

    @Override
    public ReporteDto generar(FiltroReporte f) {
        Specification<Cotizacion> spec = (r, cq, cb) -> cb.and(
                cb.greaterThanOrEqualTo(r.<LocalDate>get("fechaEmision"), f.desde()),
                cb.lessThanOrEqualTo(r.<LocalDate>get("fechaEmision"), f.hasta()));
        if (f.estado() != null) {
            EstadoCotizacion e = EstadoCotizacion.valueOf(f.estado());
            spec = spec.and((r, cq, cb) -> cb.equal(r.get("estado"), e));
        }
        List<Cotizacion> lista = cotizaciones.findAll(spec, Sort.by("fechaEmision", "id"));
        List<List<Object>> filas = new ArrayList<>();
        Map<EstadoCotizacion, Long> porEstado = new EnumMap<>(EstadoCotizacion.class);
        BigDecimal total = BigDecimal.ZERO;
        for (Cotizacion c : lista) {
            filas.add(List.of(c.getCodigo(), Formato.fecha(c.getFechaEmision()), c.getCliente().getNombre(),
                    c.getVehiculo() == null ? "" : c.getVehiculo().getPlaca(), Formato.fecha(c.getVigenciaHasta()),
                    c.getEstado().name(), c.getTotal()));
            porEstado.merge(c.getEstado(), 1L, Long::sum);
            total = total.add(c.getTotal());
        }
        Map<String, Object> resumen = new LinkedHashMap<>();
        resumen.put("Cotizaciones", (long) lista.size());
        porEstado.forEach((k, v) -> resumen.put(k.name(), v));
        resumen.put("Monto cotizado (S/)", total);
        return new ReporteDto(tipo(), "Cotizaciones", f.periodo(),
                List.of("Código", "Emisión", "Cliente", "Placa", "Vigencia", "Estado", "Total (S/)"), filas, resumen);
    }
}
