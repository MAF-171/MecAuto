package pe.mecauto.service;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.mecauto.dto.SeguimientoDtos.SeguimientoOrdenDto;
import pe.mecauto.entity.OrdenTrabajo;
import pe.mecauto.exception.RecursoNoEncontradoException;
import pe.mecauto.repository.DetalleOrdenServicioRepository;
import pe.mecauto.repository.OrdenTrabajoRepository;
import pe.mecauto.util.PlacaUtil;

import java.util.Locale;

/**
 * Consulta pública del estado de una orden. Para evitar que alguien adivine códigos consecutivos (OT-0001,
 * OT-0002...) se exige además los últimos 3 caracteres de la placa, y cualquier fallo responde con el
 * mismo mensaje (no se revela si el código existe). Devuelve solo datos mínimos y la placa enmascarada.
 */
@Service
public class SeguimientoService {

    static final String NO_ENCONTRADA = "No encontramos una orden con esos datos. Revisa el código y los últimos 3 caracteres de la placa.";
    private static final int DIGITOS_PLACA = 3;

    private final OrdenTrabajoRepository ordenes;
    private final DetalleOrdenServicioRepository detalles;

    public SeguimientoService(OrdenTrabajoRepository ordenes, DetalleOrdenServicioRepository detalles) {
        this.ordenes = ordenes;
        this.detalles = detalles;
    }

    @Transactional(readOnly = true)
    public SeguimientoOrdenDto consultar(String codigo, String placa) {
        String cod = StringUtils.upperCase(StringUtils.deleteWhitespace(codigo), Locale.ROOT);
        String ingresada = soloAlfanumerico(PlacaUtil.normalizar(placa));
        if (StringUtils.isEmpty(cod) || ingresada.length() < DIGITOS_PLACA) {
            throw new RecursoNoEncontradoException(NO_ENCONTRADA);
        }
        OrdenTrabajo orden = ordenes.findByCodigoIgnoreCase(cod).orElseThrow(() -> new RecursoNoEncontradoException(NO_ENCONTRADA));

        String placaOrden = soloAlfanumerico(PlacaUtil.normalizar(orden.getVehiculo().getPlaca()));
        String finalOrden = StringUtils.right(placaOrden, DIGITOS_PLACA);
        if (!finalOrden.equals(StringUtils.right(ingresada, DIGITOS_PLACA))) {
            throw new RecursoNoEncontradoException(NO_ENCONTRADA);
        }

        String servicio = detalles.findFirstByOrdenIdOrderByIdAsc(orden.getId()).map(d -> d.getDescripcion()).orElse(null);
        return new SeguimientoOrdenDto(orden.getCodigo(), orden.getEstado(), "***-" + finalOrden, servicio,
                orden.getFechaIngreso(), orden.getFechaEntregaEstimada());
    }

    private static String soloAlfanumerico(String s) {
        return s == null ? "" : s.replaceAll("[^A-Z0-9]", "");
    }
}
