package pe.mecauto.service.reporte;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pe.mecauto.dto.ReporteDto;
import pe.mecauto.entity.Cliente;
import pe.mecauto.repository.ClienteRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Clientes registrados en el periodo. */
@Component
@Transactional(readOnly = true)
public class ClientesReporte implements ReporteGenerador {

    private final ClienteRepository clientes;

    public ClientesReporte(ClienteRepository clientes) {
        this.clientes = clientes;
    }

    @Override
    public String tipo() {
        return "clientes";
    }

    @Override
    public ReporteDto generar(FiltroReporte f) {
        Specification<Cliente> spec = (r, cq, cb) -> cb.and(
                cb.greaterThanOrEqualTo(r.<LocalDateTime>get("creadoEn"), f.inicio()),
                cb.lessThan(r.<LocalDateTime>get("creadoEn"), f.finExclusivo()));
        List<Cliente> lista = clientes.findAll(spec, Sort.by("creadoEn"));
        List<List<Object>> filas = new ArrayList<>();
        for (Cliente c : lista) {
            filas.add(List.of(c.getNombre(), c.getTipoDocumento().name(), c.getNroDocumento(),
                    Formato.texto(c.getTelefono()), Formato.texto(c.getCorreo()), c.isActivo() ? "Activo" : "Inactivo",
                    Formato.fechaHora(c.getCreadoEn())));
        }
        Map<String, Object> resumen = new LinkedHashMap<>();
        resumen.put("Clientes registrados en el periodo", (long) lista.size());
        resumen.put("Total de clientes activos hoy", clientes.countByActivoTrue());
        return new ReporteDto(tipo(), "Clientes registrados", f.periodo(),
                List.of("Nombre / razón social", "Tipo doc.", "Documento", "Teléfono", "Correo", "Estado", "Registro"), filas, resumen);
    }
}
