package pe.mecauto.service.reporte;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pe.mecauto.dto.ReporteDto;
import pe.mecauto.entity.Repuesto;
import pe.mecauto.repository.RepuestoRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Foto del momento: repuestos activos con stock menor o igual al mínimo. No depende del periodo. */
@Component
@Transactional(readOnly = true)
public class StockBajoReporte implements ReporteGenerador {

    private final RepuestoRepository repuestos;

    public StockBajoReporte(RepuestoRepository repuestos) {
        this.repuestos = repuestos;
    }

    @Override
    public String tipo() {
        return "stock-bajo";
    }

    @Override
    public ReporteDto generar(FiltroReporte f) {
        List<List<Object>> filas = new ArrayList<>();
        for (Repuesto r : repuestos.listarStockBajo()) {
            filas.add(List.of(r.getCodigo(), r.getNombre(), Formato.texto(r.getCategoria()), (long) r.getStockActual(),
                    (long) r.getStockMinimo(), (long) Math.max(0, r.getStockMinimo() - r.getStockActual()),
                    r.getProveedor() == null ? "" : r.getProveedor().getRazonSocial()));
        }
        Map<String, Object> resumen = new LinkedHashMap<>();
        resumen.put("Repuestos con stock bajo", (long) filas.size());
        return new ReporteDto(tipo(), "Repuestos con stock bajo", "Situación actual",
                List.of("Código", "Repuesto", "Categoría", "Stock", "Mínimo", "Faltante", "Proveedor"), filas, resumen);
    }
}
