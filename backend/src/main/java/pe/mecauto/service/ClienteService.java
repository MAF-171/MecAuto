package pe.mecauto.service;

import com.google.common.collect.Sets;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.mecauto.dao.ClienteDAO;
import pe.mecauto.domain.EstadoOrden;
import pe.mecauto.dto.PageDto;
import pe.mecauto.dto.PersonaDtos.ClienteDto;
import pe.mecauto.dto.PersonaDtos.ClienteRequest;
import pe.mecauto.dto.PersonaDtos.VehiculoDto;
import pe.mecauto.entity.Cliente;
import pe.mecauto.exception.ConflictoException;
import pe.mecauto.exception.NegocioException;
import pe.mecauto.exception.RecursoNoEncontradoException;
import pe.mecauto.exception.ValidacionException;
import pe.mecauto.mapper.PersonaMapper;
import pe.mecauto.repository.ClienteRepository;
import pe.mecauto.repository.OrdenTrabajoRepository;
import pe.mecauto.repository.VehiculoRepository;
import pe.mecauto.util.DocumentoValidator;
import pe.mecauto.util.TextoUtil;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class ClienteService {

    /** Estados en los que la orden sigue abierta (colección inmutable de Guava). */
    private static final Set<EstadoOrden> ACTIVOS = Sets.immutableEnumSet(
            EstadoOrden.RECIBIDA, EstadoOrden.EN_DIAGNOSTICO, EstadoOrden.EN_REPARACION, EstadoOrden.COMPLETADA);

    private final ClienteRepository repo;
    private final VehiculoRepository vehiculos;
    private final OrdenTrabajoRepository ordenes;
    private final ClienteDAO clienteDao;

    public ClienteService(ClienteRepository repo, VehiculoRepository vehiculos, OrdenTrabajoRepository ordenes,
                          ClienteDAO clienteDao) {
        this.repo = repo;
        this.vehiculos = vehiculos;
        this.ordenes = ordenes;
        this.clienteDao = clienteDao;
    }

    public PageDto<ClienteDto> listar(String q, boolean soloActivos, int pagina, int tamano) {
        String t = "%" + TextoUtil.limpiarBusqueda(q).toLowerCase(Locale.ROOT) + "%";
        Specification<Cliente> spec = (r, cq, cb) -> cb.or(
                cb.like(cb.lower(r.get("nombre")), t),
                cb.like(cb.lower(r.get("nroDocumento")), t),
                cb.like(cb.lower(cb.coalesce(r.<String>get("telefono"), "")), t));
        if (soloActivos) {
            spec = spec.and((r, cq, cb) -> cb.isTrue(r.get("activo")));
        }
        Page<Cliente> page = repo.findAll(spec, PageRequest.of(pagina, Math.min(tamano, 100), Sort.by("nombre")));
        return PageDto.de(page, PersonaMapper::aDto);
    }

    public ClienteDto obtener(Long id) {
        Cliente c = buscar(id);
        return PersonaMapper.aDto(c, vehiculos.countByClienteIdAndActivoTrue(id));
    }

    public List<VehiculoDto> vehiculosDe(Long id) {
        buscar(id);
        return vehiculos.findByClienteIdOrderByPlacaAsc(id).stream().map(PersonaMapper::aDto).toList();
    }

    @Transactional
    public ClienteDto crear(ClienteRequest req) {
        String doc = normalizarDocumento(req);
        if (clienteDao.existePorDocumento(doc)) {
            throw new ConflictoException("Ya existe un cliente con ese documento");
        }
        Cliente c = new Cliente();
        aplicar(c, req, doc);
        return PersonaMapper.aDto(repo.save(c));
    }

    @Transactional
    public ClienteDto actualizar(Long id, ClienteRequest req) {
        Cliente c = buscar(id);
        String doc = normalizarDocumento(req);
        if (clienteDao.existeOtroConDocumento(doc, id)) {
            throw new ConflictoException("Ya existe otro cliente con ese documento");
        }
        aplicar(c, req, doc);
        return PersonaMapper.aDto(c);
    }

    /** Desactiva (o reactiva) al cliente. No se borra: su historial se conserva. */
    @Transactional
    public ClienteDto cambiarEstado(Long id, boolean activo) {
        Cliente c = buscar(id);
        if (!activo && ordenes.existsByClienteIdAndEstadoIn(id, ACTIVOS)) {
            throw new NegocioException("El cliente tiene órdenes de trabajo abiertas; ciérralas o cancélalas primero");
        }
        c.setActivo(activo);
        return PersonaMapper.aDto(c);
    }

    private String normalizarDocumento(ClienteRequest req) {
        String doc = req.nroDocumento().trim().toUpperCase(Locale.ROOT);
        if (!DocumentoValidator.esValido(req.tipoDocumento(), doc)) {
            throw new ValidacionException("nroDocumento", DocumentoValidator.mensajeFormato(req.tipoDocumento()));
        }
        return doc;
    }

    private void aplicar(Cliente c, ClienteRequest req, String doc) {
        c.setTipoDocumento(req.tipoDocumento());
        c.setNroDocumento(doc);
        c.setNombre(TextoUtil.normalizarEspacios(req.nombre()));
        c.setTelefono(TextoUtil.aNullSiVacio(req.telefono()));
        c.setCorreo(TextoUtil.aNullSiVacio(req.correo()));
        c.setDireccion(TextoUtil.aNullSiVacio(req.direccion()));
    }

    private Cliente buscar(Long id) {
        return repo.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Cliente", id));
    }
}
