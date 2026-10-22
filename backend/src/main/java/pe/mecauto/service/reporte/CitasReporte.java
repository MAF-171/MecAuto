package pe.mecauto.service.reporte;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pe.mecauto.domain.EstadoCita;
import pe.mecauto.dto.ReporteDto;
import pe.mecauto.entity.Cita;
import pe.mecauto.repository.CitaRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@Transactional(readOnly = true)
public class CitasReporte implements ReporteGenerador {

    private final CitaRepository citas;

    public CitasReporte(CitaRepository citas) {
        this.citas = citas;
    }

    @Override
    public String tipo() {
        return "citas";
    }

    @Override
    public ReporteDto generar(FiltroReporte f) {
        Specification<Cita> spec = (r, cq, cb) -> cb.and(
                cb.greaterThanOrEqualTo(r.<LocalDateTime>get("fechaHora"), f.inicio()),
                cb.lessThan(r.<LocalDateTime>get("fechaHora"), f.finExclusivo()));
        if (f.estado() != null) {
            EstadoCita e = EstadoCita.valueOf(f.estado());
            spec = spec.and((r, cq, cb) -> cb.equal(r.get("estado"), e));
        }
        List<Cita> lista = citas.findAll(spec, Sort.by("fechaHora"));
        List<List<Object>> filas = new ArrayList<>();
        Map<EstadoCita, Long> porEstado = new EnumMap<>(EstadoCita.class);
        for (Cita c : lista) {
            filas.add(List.of(c.getCodigo(), Formato.fechaHora(c.getFechaHora()), c.getNombreContacto(),
                    c.getTelefonoContacto(), c.getPlaca(), c.getServicio().getNombre(), c.getEstado().name(),
                    c.getOrigen().name()));
            porEstado.merge(c.getEstado(), 1L, Long::sum);
        }
        Map<String, Object> resumen = new LinkedHashMap<>();
        resumen.put("Citas", (long) lista.size());
        porEstado.forEach((k, v) -> resumen.put(k.name(), v));
        return new ReporteDto(tipo(), "Citas por periodo y estado", f.periodo(),
                List.of("Código", "Fecha y hora", "Contacto", "Teléfono", "Placa", "Servicio", "Estado", "Origen"), filas, resumen);
    }
}
