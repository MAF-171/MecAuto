package pe.mecauto.service;

import org.springframework.stereotype.Service;
import pe.mecauto.dto.ReporteDto;
import pe.mecauto.exception.NegocioException;
import pe.mecauto.exception.RecursoNoEncontradoException;
import pe.mecauto.service.reporte.FiltroReporte;
import pe.mecauto.service.reporte.ReporteGenerador;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Fábrica/registro de reportes: Spring entrega todas las estrategias y aquí se elige por nombre. */
@Service
public class ReporteService {

    private final Map<String, ReporteGenerador> generadores;

    public ReporteService(List<ReporteGenerador> lista) {
        this.generadores = lista.stream().collect(Collectors.toMap(ReporteGenerador::tipo, Function.identity()));
    }

    public List<String> tiposDisponibles() {
        return generadores.keySet().stream().sorted().toList();
    }

    public ReporteDto generar(String tipo, LocalDate desde, LocalDate hasta, String estado) {
        ReporteGenerador g = generadores.get(tipo);
        if (g == null) {
            throw new RecursoNoEncontradoException("Reporte \"" + tipo + "\" no existe");
        }
        try {
            return g.generar(new FiltroReporte(desde, hasta, estado).normalizado());
        } catch (IllegalArgumentException e) {
            throw new NegocioException("El filtro de estado no es válido para este reporte");
        }
    }
}
