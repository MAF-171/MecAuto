package pe.mecauto.service.reporte;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pe.mecauto.domain.EstadoComprobante;
import pe.mecauto.dto.ReporteDto;
import pe.mecauto.entity.Comprobante;
import pe.mecauto.repository.ComprobanteRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Ingresos registrados = suma de comprobantes EMITIDOS (los anulados no cuentan), agrupados por día. */
@Component
@Transactional(readOnly = true)
public class IngresosReporte implements ReporteGenerador {

    private final ComprobanteRepository comprobantes;

    public IngresosReporte(ComprobanteRepository comprobantes) {
        this.comprobantes = comprobantes;
    }

    @Override
    public String tipo() {
        return "ingresos";
    }

    @Override
    public ReporteDto generar(FiltroReporte f) {
        List<Comprobante> lista = comprobantes
                .findByEstadoAndFechaEmisionGreaterThanEqualAndFechaEmisionLessThanOrderByFechaEmisionAsc(
                        EstadoComprobante.EMITIDO, f.inicio(), f.finExclusivo());
        Map<LocalDate, BigDecimal[]> porDia = new TreeMap<>(); // [cantidad, base, igv, total]
        for (Comprobante c : lista) {
            BigDecimal[] a = porDia.computeIfAbsent(c.getFechaEmision().toLocalDate(),
                    k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});
            a[0] = a[0].add(BigDecimal.ONE);
            a[1] = a[1].add(c.getBaseImponible());
            a[2] = a[2].add(c.getIgv());
            a[3] = a[3].add(c.getTotal());
        }
        List<List<Object>> filas = new ArrayList<>();
        BigDecimal base = BigDecimal.ZERO;
        BigDecimal igv = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<LocalDate, BigDecimal[]> e : porDia.entrySet()) {
            BigDecimal[] a = e.getValue();
            filas.add(List.of(Formato.fecha(e.getKey()), a[0].longValue(), a[1], a[2], a[3]));
            base = base.add(a[1]);
            igv = igv.add(a[2]);
            total = total.add(a[3]);
        }
        Map<String, Object> resumen = new LinkedHashMap<>();
        resumen.put("Comprobantes emitidos", (long) lista.size());
        resumen.put("Base imponible (S/)", base);
        resumen.put("IGV (S/)", igv);
        resumen.put("Ingresos totales (S/)", total);
        return new ReporteDto(tipo(), "Ingresos registrados", f.periodo(),
                List.of("Fecha", "Comprobantes", "Base imponible (S/)", "IGV (S/)", "Total (S/)"), filas, resumen);
    }
}
