package pe.mecauto.service.reporte;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pe.mecauto.domain.EstadoOrden;
import pe.mecauto.dto.ReporteDto;
import pe.mecauto.repository.DetalleOrdenServicioRepository;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Servicios más solicitados según las órdenes del periodo (sin las canceladas). */
@Component
@Transactional(readOnly = true)
public class ServiciosSolicitadosReporte implements ReporteGenerador {

    private final DetalleOrdenServicioRepository detalles;

    public ServiciosSolicitadosReporte(DetalleOrdenServicioRepository detalles) {
        this.detalles = detalles;
    }

    @Override
    public String tipo() {
        return "servicios-solicitados";
    }

    @Override
    public ReporteDto generar(FiltroReporte f) {
        List<List<Object>> filas = new ArrayList<>();
        long total = 0;
        for (Object[] fila : detalles.serviciosMasSolicitados(
                EnumSet.complementOf(EnumSet.of(EstadoOrden.CANCELADA)), f.inicio(), f.finExclusivo())) {
            long cantidad = ((Number) fila[1]).longValue();
            filas.add(List.of((String) fila[0], cantidad));
            total += cantidad;
        }
        Map<String, Object> resumen = new LinkedHashMap<>();
        resumen.put("Servicios distintos", (long) filas.size());
        resumen.put("Unidades de servicio", total);
        return new ReporteDto(tipo(), "Servicios más solicitados", f.periodo(), List.of("Servicio", "Cantidad"), filas, resumen);
    }
}
