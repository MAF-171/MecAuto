package pe.mecauto.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.mecauto.domain.EstadoComprobante;
import pe.mecauto.domain.EstadoOrden;
import pe.mecauto.domain.TipoComprobante;
import pe.mecauto.dto.ComprobanteDtos.AnularComprobanteRequest;
import pe.mecauto.dto.ComprobanteDtos.ComprobanteDto;
import pe.mecauto.dto.ComprobanteDtos.EmitirComprobanteRequest;
import pe.mecauto.dto.PageDto;
import pe.mecauto.entity.Comprobante;
import pe.mecauto.entity.OrdenTrabajo;
import pe.mecauto.exception.ConflictoException;
import pe.mecauto.exception.NegocioException;
import pe.mecauto.exception.RecursoNoEncontradoException;
import pe.mecauto.mapper.OperacionMapper;
import pe.mecauto.repository.ComprobanteRepository;
import pe.mecauto.repository.OrdenTrabajoRepository;
import pe.mecauto.repository.UsuarioRepository;
import pe.mecauto.security.UsuarioPrincipal;
import pe.mecauto.util.TextoUtil;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;

/**
 * Comprobantes INTERNOS (boleta/factura). No son comprobantes electrónicos autorizados por SUNAT.
 * Reglas: RN05 (uno por orden, solo si está COMPLETADA), numeración correlativa por serie sin saltos duplicados.
 */
@Service
@Transactional(readOnly = true)
public class ComprobanteService {

    private final ComprobanteRepository comprobantes;
    private final OrdenTrabajoRepository ordenes;
    private final UsuarioRepository usuarios;
    private final SecuenciaService secuencias;
    private final ConfiguracionService config;
    private final ComprobanteFactory factory;

    public ComprobanteService(ComprobanteRepository comprobantes, OrdenTrabajoRepository ordenes,
                              UsuarioRepository usuarios, SecuenciaService secuencias, ConfiguracionService config,
                              ComprobanteFactory factory) {
        this.comprobantes = comprobantes;
        this.ordenes = ordenes;
        this.usuarios = usuarios;
        this.secuencias = secuencias;
        this.config = config;
        this.factory = factory;
    }

    public PageDto<ComprobanteDto> listar(String q, TipoComprobante tipo, EstadoComprobante estado, LocalDate desde,
                                          LocalDate hasta, int pagina, int tamano) {
        Specification<Comprobante> spec = (r, cq, cb) -> cb.conjunction();
        String texto = TextoUtil.limpiarBusqueda(q).toLowerCase(Locale.ROOT);
        if (!texto.isEmpty()) {
            String t = "%" + texto + "%";
            spec = spec.and((r, cq, cb) -> cb.or(
                    cb.like(cb.lower(r.get("clienteNombre")), t),
                    cb.like(cb.lower(r.get("clienteDocumento")), t),
                    cb.like(cb.lower(r.get("numero")), t),
                    cb.like(cb.lower(r.get("vehiculoPlaca")), t)));
        }
        if (tipo != null) {
            spec = spec.and((r, cq, cb) -> cb.equal(r.get("tipo"), tipo));
        }
        if (estado != null) {
            spec = spec.and((r, cq, cb) -> cb.equal(r.get("estado"), estado));
        }
        if (desde != null) {
            spec = spec.and((r, cq, cb) -> cb.greaterThanOrEqualTo(r.<LocalDateTime>get("fechaEmision"), desde.atStartOfDay()));
        }
        if (hasta != null) {
            spec = spec.and((r, cq, cb) -> cb.lessThan(r.<LocalDateTime>get("fechaEmision"), hasta.plusDays(1).atStartOfDay()));
        }
        Page<Comprobante> page = comprobantes.findAll(spec,
                PageRequest.of(pagina, Math.min(tamano, 100), Sort.by(Sort.Direction.DESC, "fechaEmision", "id")));
        return PageDto.de(page, c -> OperacionMapper.aDto(c, null));
    }

    public ComprobanteDto obtener(Long id) {
        return OperacionMapper.aDto(buscar(id), config.datosEmisor());
    }

    @Transactional
    public ComprobanteDto emitir(EmitirComprobanteRequest req, UsuarioPrincipal actor) {
        OrdenTrabajo o = ordenes.findByIdForUpdate(req.ordenId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Orden de trabajo", req.ordenId()));
        if (o.getEstado() != EstadoOrden.COMPLETADA) {
            throw new NegocioException("Solo se emite comprobante de una orden COMPLETADA (la orden está " + o.getEstado() + ")");
        }
        if (comprobantes.existsByOrdenIdAndEstado(o.getId(), EstadoComprobante.EMITIDO)) {
            throw new ConflictoException("La orden ya tiene un comprobante emitido");
        }
        if (o.getTotal().signum() <= 0) {
            throw new NegocioException("La orden tiene total cero; no se puede emitir comprobante");
        }
        TipoComprobante tipo = factory.tipoPara(o);
        String serie = config.obtener(tipo == TipoComprobante.FACTURA ? "serie_factura" : "serie_boleta");
        String numero = String.format("%08d", secuencias.siguiente(serie));
        Comprobante c = factory.crear(o, req.metodoPago(), usuarios.getReferenceById(actor.getId()), serie, numero);
        comprobantes.save(c);
        return OperacionMapper.aDto(c, config.datosEmisor());
    }

    @Transactional
    public ComprobanteDto anular(Long id, AnularComprobanteRequest req) {
        Comprobante c = buscar(id);
        if (c.getEstado() == EstadoComprobante.ANULADO) {
            throw new NegocioException("El comprobante ya está anulado");
        }
        if (c.getOrden().getEstado() == EstadoOrden.ENTREGADA) {
            throw new NegocioException("La orden ya fue entregada; no se puede anular su comprobante");
        }
        c.setEstado(EstadoComprobante.ANULADO);
        c.setMotivoAnulacion(req.motivo().trim());
        return OperacionMapper.aDto(c, config.datosEmisor());
    }

    private Comprobante buscar(Long id) {
        return comprobantes.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Comprobante", id));
    }
}
