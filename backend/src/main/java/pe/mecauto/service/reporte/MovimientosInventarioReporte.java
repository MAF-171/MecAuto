package pe.mecauto.service.reporte;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pe.mecauto.domain.TipoMovimiento;
import pe.mecauto.dto.ReporteDto;
import pe.mecauto.entity.MovimientoInventario;
import pe.mecauto.repository.MovimientoInventarioRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Repuestos: movimientos de inventario del periodo. El filtro "estado" admite ENTRADA, SALIDA o AJUSTE. */
@Component
@Transactional(readOnly = true)
public class MovimientosInventarioReporte implements ReporteGenerador {

    private final MovimientoInventarioRepository movimientos;

    public MovimientosInventarioReporte(MovimientoInventarioRepository movimientos) {
        this.movimientos = movimientos;
    }

    @Override
    public String tipo() {
        return "inventario-movimientos";
    }

    @Override
    public ReporteDto generar(FiltroReporte f) {
        Specification<MovimientoInventario> spec = (r, cq, cb) -> cb.and(
                cb.greaterThanOrEqualTo(r.<LocalDateTime>get("fecha"), f.inicio()),
                cb.lessThan(r.<LocalDateTime>get("fecha"), f.finExclusivo()));
        if (f.estado() != null) {
            TipoMovimiento t = TipoMovimiento.valueOf(f.estado());
            spec = spec.and((r, cq, cb) -> cb.equal(r.get("tipo"), t));
        }
        List<MovimientoInventario> lista = movimientos.findAll(spec, Sort.by("fecha", "id"));
        List<List<Object>> filas = new ArrayList<>();
        long entradas = 0;
        long salidas = 0;
        for (MovimientoInventario m : lista) {
            filas.add(List.of(Formato.fechaHora(m.getFecha()), m.getRepuesto().getCodigo(), m.getRepuesto().getNombre(),
                    m.getTipo().name(), (long) m.getCantidad(), (long) m.getStockResultante(), Formato.texto(m.getMotivo()),
                    m.getUsuario() == null ? "" : m.getUsuario().getNombre()));
            if (m.getCantidad() > 0) {
                entradas += m.getCantidad();
            } else {
                salidas += -m.getCantidad();
            }
        }
        Map<String, Object> resumen = new LinkedHashMap<>();
        resumen.put("Movimientos", (long) lista.size());
        resumen.put("Unidades que ingresaron", entradas);
        resumen.put("Unidades que salieron", salidas);
        return new ReporteDto(tipo(), "Movimientos de inventario", f.periodo(),
                List.of("Fecha", "Código", "Repuesto", "Tipo", "Cantidad", "Stock resultante", "Motivo", "Usuario"), filas, resumen);
    }
}
