package pe.mecauto.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.mecauto.domain.EstadoOrden;
import pe.mecauto.dto.PageDto;
import pe.mecauto.dto.PersonaDtos.HistorialMantenimientoDto;
import pe.mecauto.dto.PersonaDtos.VehiculoDto;
import pe.mecauto.dto.PersonaDtos.VehiculoRequest;
import pe.mecauto.entity.Cliente;
import pe.mecauto.entity.OrdenTrabajo;
import pe.mecauto.entity.Vehiculo;
import pe.mecauto.exception.ConflictoException;
import pe.mecauto.exception.NegocioException;
import pe.mecauto.exception.RecursoNoEncontradoException;
import pe.mecauto.exception.ValidacionException;
import pe.mecauto.mapper.PersonaMapper;
import pe.mecauto.repository.ClienteRepository;
import pe.mecauto.repository.OrdenTrabajoRepository;
import pe.mecauto.repository.VehiculoRepository;
import pe.mecauto.util.PlacaUtil;
import pe.mecauto.util.TextoUtil;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class VehiculoService {

    private static final EnumSet<EstadoOrden> ACTIVOS = EnumSet.of(
            EstadoOrden.RECIBIDA, EstadoOrden.EN_DIAGNOSTICO, EstadoOrden.EN_REPARACION, EstadoOrden.COMPLETADA);

    private final VehiculoRepository repo;
    private final ClienteRepository clientes;
    private final OrdenTrabajoRepository ordenes;

    public VehiculoService(VehiculoRepository repo, ClienteRepository clientes, OrdenTrabajoRepository ordenes) {
        this.repo = repo;
        this.clientes = clientes;
        this.ordenes = ordenes;
    }

    public PageDto<VehiculoDto> listar(String q, Long clienteId, boolean soloActivos, int pagina, int tamano) {
        String t = "%" + TextoUtil.limpiarBusqueda(q).toLowerCase(Locale.ROOT) + "%";
        Specification<Vehiculo> spec = (r, cq, cb) -> cb.or(
                cb.like(cb.lower(r.get("placa")), t),
                cb.like(cb.lower(r.get("marca")), t),
                cb.like(cb.lower(r.get("modelo")), t),
                cb.like(cb.lower(r.get("cliente").get("nombre")), t));
        if (clienteId != null) {
            spec = spec.and((r, cq, cb) -> cb.equal(r.get("cliente").get("id"), clienteId));
        }
        if (soloActivos) {
            spec = spec.and((r, cq, cb) -> cb.isTrue(r.get("activo")));
        }
        Page<Vehiculo> page = repo.findAll(spec, PageRequest.of(pagina, Math.min(tamano, 100), Sort.by("placa")));
        return PageDto.de(page, PersonaMapper::aDto);
    }

    public VehiculoDto obtener(Long id) {
        return PersonaMapper.aDto(buscar(id));
    }

    @Transactional
    public VehiculoDto crear(VehiculoRequest req) {
        String placa = placaValida(req.placa());
        if (repo.existsByPlaca(placa)) {
            throw new ConflictoException("Ya existe un vehículo con la placa " + placa);
        }
        Cliente cliente = clienteActivo(req.clienteId());
        Vehiculo v = new Vehiculo();
        v.setCliente(cliente);
        aplicar(v, req, placa);
        return PersonaMapper.aDto(repo.save(v));
    }

    @Transactional
    public VehiculoDto actualizar(Long id, VehiculoRequest req) {
        Vehiculo v = buscar(id);
        String placa = placaValida(req.placa());
        repo.findByPlaca(placa).filter(o -> !o.getId().equals(id)).ifPresent(o -> {
            throw new ConflictoException("Ya existe otro vehículo con la placa " + placa);
        });
        if (!v.getCliente().getId().equals(req.clienteId())) {
            v.setCliente(clienteActivo(req.clienteId()));
        }
        aplicar(v, req, placa);
        return PersonaMapper.aDto(v);
    }

    @Transactional
    public VehiculoDto cambiarEstado(Long id, boolean activo) {
        Vehiculo v = buscar(id);
        if (!activo && ordenes.existsByVehiculoIdAndEstadoIn(id, ACTIVOS)) {
            throw new NegocioException("El vehículo tiene órdenes de trabajo abiertas");
        }
        v.setActivo(activo);
        return PersonaMapper.aDto(v);
    }

    /** RF04: el historial se deriva de las órdenes de trabajo reales del vehículo (más recientes primero). */
    public List<HistorialMantenimientoDto> historial(Long id) {
        buscar(id);
        return ordenes.findByVehiculoIdOrderByFechaIngresoDesc(id).stream()
                .map(this::filaHistorial)
                .toList();
    }

    private HistorialMantenimientoDto filaHistorial(OrdenTrabajo o) {
        return new HistorialMantenimientoDto(o.getId(), o.getCodigo(), o.getFechaIngreso(), o.getFechaCierre(),
                o.getKilometrajeIngreso(), o.getFalla(), o.getDiagnostico(), o.getTrabajosRealizados(), o.getEstado(),
                o.getMecanico() == null ? null : o.getMecanico().getNombre(), o.getTotal());
    }

    private String placaValida(String entrada) {
        String placa = PlacaUtil.normalizar(entrada);
        if (!PlacaUtil.esValida(placa)) {
            throw new ValidacionException("placa", "Placa inválida (ej.: ABC-123)");
        }
        return placa;
    }

    private Cliente clienteActivo(Long clienteId) {
        Cliente c = clientes.findById(clienteId).orElseThrow(() -> new RecursoNoEncontradoException("Cliente", clienteId));
        if (!c.isActivo()) {
            throw new NegocioException("El cliente está desactivado");
        }
        return c;
    }

    private void aplicar(Vehiculo v, VehiculoRequest req, String placa) {
        v.setPlaca(placa);
        v.setMarca(req.marca().trim());
        v.setModelo(req.modelo().trim());
        v.setAnio(req.anio());
        v.setColor(TextoUtil.aNullSiVacio(req.color()));
        v.setKilometraje(req.kilometraje());
    }

    private Vehiculo buscar(Long id) {
        return repo.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Vehículo", id));
    }
}
