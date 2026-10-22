package pe.mecauto.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.mecauto.domain.CalculadoraIgv;
import pe.mecauto.domain.EstadoCita;
import pe.mecauto.domain.EstadoComprobante;
import pe.mecauto.domain.EstadoOrden;
import pe.mecauto.domain.OrdenEstadoPolicy;
import pe.mecauto.domain.Rol;
import pe.mecauto.dto.OrdenDtos.*;
import pe.mecauto.dto.PageDto;
import pe.mecauto.entity.Cita;
import pe.mecauto.entity.Comprobante;
import pe.mecauto.entity.DetalleOrdenRepuesto;
import pe.mecauto.entity.DetalleOrdenServicio;
import pe.mecauto.entity.OrdenHistorial;
import pe.mecauto.entity.OrdenTrabajo;
import pe.mecauto.entity.Repuesto;
import pe.mecauto.entity.Servicio;
import pe.mecauto.entity.Usuario;
import pe.mecauto.entity.Vehiculo;
import pe.mecauto.exception.AccesoDenegadoNegocioException;
import pe.mecauto.exception.ConflictoException;
import pe.mecauto.exception.NegocioException;
import pe.mecauto.exception.RecursoNoEncontradoException;
import pe.mecauto.exception.ValidacionException;
import pe.mecauto.mapper.OperacionMapper;
import pe.mecauto.repository.CitaRepository;
import pe.mecauto.repository.ComprobanteRepository;
import pe.mecauto.repository.OrdenHistorialRepository;
import pe.mecauto.repository.OrdenTrabajoRepository;
import pe.mecauto.repository.RepuestoRepository;
import pe.mecauto.repository.ServicioRepository;
import pe.mecauto.repository.UsuarioRepository;
import pe.mecauto.repository.VehiculoRepository;
import pe.mecauto.security.UsuarioPrincipal;
import pe.mecauto.util.Tiempo;
import pe.mecauto.util.TextoUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Órdenes de trabajo: creación, asignación, detalle (servicios y repuestos), cambios de estado y descuento de inventario.
 * Todas las operaciones que cambian la orden bloquean su fila (findByIdForUpdate): dos personas no pueden
 * completar la misma orden a la vez, así el inventario nunca se descuenta dos veces.
 */
@Service
@Transactional(readOnly = true)
public class OrdenService {

    private static final Set<EstadoOrden> ABIERTAS = EnumSet.of(
            EstadoOrden.RECIBIDA, EstadoOrden.EN_DIAGNOSTICO, EstadoOrden.EN_REPARACION, EstadoOrden.COMPLETADA);

    private final OrdenTrabajoRepository ordenes;
    private final OrdenHistorialRepository historiales;
    private final VehiculoRepository vehiculos;
    private final UsuarioRepository usuarios;
    private final ServicioRepository servicios;
    private final RepuestoRepository repuestos;
    private final CitaRepository citas;
    private final ComprobanteRepository comprobantes;
    private final InventarioService inventario;
    private final SecuenciaService secuencias;

    public OrdenService(OrdenTrabajoRepository ordenes, OrdenHistorialRepository historiales,
                        VehiculoRepository vehiculos, UsuarioRepository usuarios, ServicioRepository servicios,
                        RepuestoRepository repuestos, CitaRepository citas, ComprobanteRepository comprobantes,
                        InventarioService inventario, SecuenciaService secuencias) {
        this.ordenes = ordenes;
        this.historiales = historiales;
        this.vehiculos = vehiculos;
        this.usuarios = usuarios;
        this.servicios = servicios;
        this.repuestos = repuestos;
        this.citas = citas;
        this.comprobantes = comprobantes;
        this.inventario = inventario;
        this.secuencias = secuencias;
    }

    // ------------------------------------------------------------------ Consultas

    public PageDto<OrdenResumenDto> listar(String q, EstadoOrden estado, Long mecanicoId, LocalDate desde,
                                           LocalDate hasta, int pagina, int tamano, UsuarioPrincipal actor) {
        Specification<OrdenTrabajo> spec = (r, cq, cb) -> cb.conjunction();
        String texto = TextoUtil.limpiarBusqueda(q).toLowerCase(Locale.ROOT);
        if (!texto.isEmpty()) {
            String t = "%" + texto + "%";
            spec = spec.and((r, cq, cb) -> cb.or(
                    cb.like(cb.lower(r.get("codigo")), t),
                    cb.like(cb.lower(r.get("falla")), t),
                    cb.like(cb.lower(r.get("vehiculo").get("placa")), t),
                    cb.like(cb.lower(r.get("cliente").get("nombre")), t)));
        }
        if (estado != null) {
            spec = spec.and((r, cq, cb) -> cb.equal(r.get("estado"), estado));
        }
        // El mecánico solo ve sus órdenes, sin importar lo que pida el cliente HTTP.
        Long filtroMecanico = actor.getRol() == Rol.MECANICO ? actor.getId() : mecanicoId;
        if (filtroMecanico != null) {
            spec = spec.and((r, cq, cb) -> cb.equal(r.get("mecanico").get("id"), filtroMecanico));
        }
        if (desde != null) {
            spec = spec.and((r, cq, cb) -> cb.greaterThanOrEqualTo(r.<LocalDateTime>get("fechaIngreso"), desde.atStartOfDay()));
        }
        if (hasta != null) {
            spec = spec.and((r, cq, cb) -> cb.lessThan(r.<LocalDateTime>get("fechaIngreso"), hasta.plusDays(1).atStartOfDay()));
        }
        Page<OrdenTrabajo> page = ordenes.findAll(spec,
                PageRequest.of(pagina, Math.min(tamano, 200), Sort.by(Sort.Direction.DESC, "fechaIngreso", "id")));
        return PageDto.de(page, OperacionMapper::aResumen);
    }

    public OrdenDetalleDto obtener(Long id, UsuarioPrincipal actor) {
        OrdenTrabajo o = buscar(id);
        exigirAcceso(o, actor);
        return aDetalle(o, actor);
    }

    // ------------------------------------------------------------------ Creación

    @Transactional
    public OrdenDetalleDto crear(OrdenCrearRequest req, UsuarioPrincipal actor) {
        Vehiculo v = vehiculos.findById(req.vehiculoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Vehículo", req.vehiculoId()));
        if (!v.isActivo() || !v.getCliente().isActivo()) {
            throw new NegocioException("El vehículo o su propietario están desactivados");
        }
        if (ordenes.existsByVehiculoIdAndEstadoIn(v.getId(), ABIERTAS)) {
            throw new ConflictoException("El vehículo " + v.getPlaca() + " ya tiene una orden de trabajo abierta");
        }
        Usuario mecanico = req.mecanicoId() == null ? null : mecanicoValido(req.mecanicoId());
        Cita cita = null;
        if (req.citaId() != null) {
            cita = citas.findById(req.citaId()).orElseThrow(() -> new RecursoNoEncontradoException("Cita", req.citaId()));
            if (!cita.getEstado().estaAbierta()) {
                throw new NegocioException("La cita ya no está abierta");
            }
            if (cita.getVehiculo() != null && !cita.getVehiculo().getId().equals(v.getId())) {
                throw new ValidacionException("citaId", "La cita pertenece a otro vehículo");
            }
        }

        OrdenTrabajo o = new OrdenTrabajo();
        o.setCodigo(secuencias.siguienteCodigo("OT"));
        o.setCliente(v.getCliente());
        o.setVehiculo(v);
        o.setMecanico(mecanico);
        o.setFalla(req.falla().trim());
        o.setFechaEntregaEstimada(req.fechaEntregaEstimada());
        o.setCreadoPor(usuarios.getReferenceById(actor.getId()));
        o.setEstado(EstadoOrden.RECIBIDA);
        if (req.kilometrajeIngreso() != null) {
            registrarKilometraje(o, v, req.kilometrajeIngreso());
        }
        if (cita != null) {
            o.setCitaId(cita.getId());
            cita.setEstado(EstadoCita.COMPLETADA);
        }
        ordenes.save(o);
        registrarHistorial(o, null, EstadoOrden.RECIBIDA, actor, cita == null ? "Orden creada" : "Orden creada desde la cita " + cita.getCodigo());
        return aDetalle(o, actor);
    }

    /** Convierte una cita (ya vinculada a cliente y vehículo) en una orden en estado RECIBIDA. */
    @Transactional
    public OrdenDetalleDto crearDesdeCita(Long citaId, UsuarioPrincipal actor) {
        Cita c = citas.findById(citaId).orElseThrow(() -> new RecursoNoEncontradoException("Cita", citaId));
        if (c.getVehiculo() == null || c.getCliente() == null) {
            throw new NegocioException("Vincula primero la cita con un cliente y un vehículo registrados");
        }
        String falla = c.getServicio().getNombre() + (c.getMotivo() == null ? "" : ": " + c.getMotivo());
        if (falla.length() > 500) {
            falla = falla.substring(0, 500);
        }
        return crear(new OrdenCrearRequest(c.getVehiculo().getId(), falla, null, null, null, c.getId()), actor);
    }

    /**
     * Crea una orden a partir de una cotización aprobada, copiando sus servicios, repuestos, mano de obra y descuento.
     * Se ejecuta dentro de la transacción de quien la llama (la conversión es una sola operación).
     */
    @Transactional
    public OrdenDetalleDto crearDesdeCotizacion(pe.mecauto.entity.Cotizacion c, UsuarioPrincipal actor) {
        if (c.getVehiculo() == null) {
            throw new NegocioException("La cotización no tiene un vehículo asociado; edítala antes de convertirla");
        }
        OrdenDetalleDto base = crear(new OrdenCrearRequest(c.getVehiculo().getId(),
                "Según cotización " + c.getCodigo(), null, null, null, null), actor);
        OrdenTrabajo o = buscar(base.id());
        BigDecimal manoObra = BigDecimal.ZERO;
        for (pe.mecauto.entity.DetalleCotizacion d : c.getDetalles()) {
            switch (d.getTipo()) {
                case SERVICIO -> {
                    DetalleOrdenServicio l = new DetalleOrdenServicio();
                    l.setOrden(o);
                    l.setServicio(d.getServicio());
                    l.setDescripcion(d.getDescripcion());
                    l.setCantidad(d.getCantidad());
                    l.setPrecioUnitario(d.getPrecioUnitario());
                    o.getServicios().add(l);
                }
                case REPUESTO -> {
                    DetalleOrdenRepuesto l = new DetalleOrdenRepuesto();
                    l.setOrden(o);
                    l.setRepuesto(d.getRepuesto());
                    l.setCantidad(d.getCantidad());
                    l.setPrecioUnitario(d.getPrecioUnitario());
                    l.setDescontado(false);
                    o.getRepuestos().add(l);
                }
                case MANO_OBRA -> manoObra = manoObra.add(CalculadoraIgv.importe(d.getCantidad(), d.getPrecioUnitario()));
            }
        }
        o.setManoObra(manoObra);
        o.setDescuento(c.getDescuento());
        o.recalcularTotal();
        ordenes.flush();
        return aDetalle(o, actor);
    }

    // ------------------------------------------------------------------ Edición

    @Transactional
    public OrdenDetalleDto actualizar(Long id, OrdenActualizarRequest req, UsuarioPrincipal actor) {
        OrdenTrabajo o = bloquear(id);
        exigirAcceso(o, actor);
        exigirEditable(o);
        if (actor.getRol() == Rol.MECANICO) {
            // El mecánico registra diagnóstico y trabajos; precios y datos de recepción no son de su competencia.
            o.setDiagnostico(TextoUtil.aNullSiVacio(req.diagnostico()));
            o.setTrabajosRealizados(TextoUtil.aNullSiVacio(req.trabajosRealizados()));
        } else {
            o.setFalla(req.falla().trim());
            o.setDiagnostico(TextoUtil.aNullSiVacio(req.diagnostico()));
            o.setTrabajosRealizados(TextoUtil.aNullSiVacio(req.trabajosRealizados()));
            o.setManoObra(CalculadoraIgv.redondear(req.manoObra()));
            if (req.descuento() != null) {
                o.setDescuento(CalculadoraIgv.redondear(req.descuento()));
            }
            o.setFechaEntregaEstimada(req.fechaEntregaEstimada());
            if (req.kilometrajeIngreso() != null) {
                registrarKilometraje(o, o.getVehiculo(), req.kilometrajeIngreso());
            }
            o.recalcularTotal();
        }
        return aDetalle(o, actor);
    }

    @Transactional
    public OrdenDetalleDto asignarMecanico(Long id, AsignarMecanicoRequest req, UsuarioPrincipal actor) {
        OrdenTrabajo o = bloquear(id);
        if (!o.getEstado().estaActiva() || o.getEstado() == EstadoOrden.COMPLETADA) {
            throw new NegocioException("No se puede cambiar el mecánico en una orden " + o.getEstado().name().toLowerCase(Locale.ROOT));
        }
        if (req.mecanicoId() == null) {
            if (o.getEstado() != EstadoOrden.RECIBIDA) {
                throw new NegocioException("Solo se puede quitar el mecánico mientras la orden está recibida");
            }
            o.setMecanico(null);
        } else {
            o.setMecanico(mecanicoValido(req.mecanicoId()));
        }
        return aDetalle(o, actor);
    }

    // ------------------------------------------------------------ Líneas de detalle

    @Transactional
    public OrdenDetalleDto agregarServicio(Long id, AgregarServicioRequest req, UsuarioPrincipal actor) {
        OrdenTrabajo o = bloquear(id);
        exigirAcceso(o, actor);
        exigirEditable(o);
        Servicio s = servicios.findById(req.servicioId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Servicio", req.servicioId()));
        if (!s.isActivo()) {
            throw new NegocioException("El servicio está desactivado");
        }
        BigDecimal precio = s.getPrecioReferencial();
        if (req.precioUnitario() != null && actor.getRol() != Rol.MECANICO) {
            precio = req.precioUnitario();
        }
        if (precio == null) {
            throw new ValidacionException("precioUnitario",
                    "Este servicio es \"según diagnóstico\": un administrador o recepción debe indicar el precio");
        }
        DetalleOrdenServicio d = new DetalleOrdenServicio();
        d.setOrden(o);
        d.setServicio(s);
        String desc = TextoUtil.aNullSiVacio(req.descripcion());
        d.setDescripcion(desc != null ? desc : s.getNombre());
        d.setCantidad(req.cantidad());
        d.setPrecioUnitario(CalculadoraIgv.redondear(precio));
        o.getServicios().add(d);
        o.recalcularTotal();
        ordenes.flush();
        return aDetalle(o, actor);
    }

    @Transactional
    public OrdenDetalleDto quitarServicio(Long id, Long lineaId, UsuarioPrincipal actor) {
        OrdenTrabajo o = bloquear(id);
        exigirAcceso(o, actor);
        exigirEditable(o);
        boolean quitada = o.getServicios().removeIf(d -> d.getId().equals(lineaId));
        if (!quitada) {
            throw new RecursoNoEncontradoException("Línea de servicio", lineaId);
        }
        o.recalcularTotal();
        return aDetalle(o, actor);
    }

    @Transactional
    public OrdenDetalleDto agregarRepuesto(Long id, AgregarRepuestoRequest req, UsuarioPrincipal actor) {
        OrdenTrabajo o = bloquear(id);
        exigirAcceso(o, actor);
        exigirEditable(o);
        Repuesto r = repuestos.findById(req.repuestoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Repuesto", req.repuestoId()));
        if (!r.isActivo()) {
            throw new NegocioException("El repuesto está desactivado");
        }
        DetalleOrdenRepuesto existente = o.getRepuestos().stream()
                .filter(d -> d.getRepuesto().getId().equals(r.getId())).findFirst().orElse(null);
        int total = req.cantidad() + (existente == null ? 0 : existente.getCantidad());
        if (total > r.getStockActual()) {
            throw new NegocioException("Stock insuficiente de \"" + r.getNombre() + "\": disponible "
                    + r.getStockActual() + ", pedido " + total);
        }
        if (existente != null) {
            existente.setCantidad(total);
        } else {
            DetalleOrdenRepuesto d = new DetalleOrdenRepuesto();
            d.setOrden(o);
            d.setRepuesto(r);
            d.setCantidad(req.cantidad());
            d.setPrecioUnitario(r.getPrecioVenta());
            d.setDescontado(false);
            o.getRepuestos().add(d);
        }
        o.recalcularTotal();
        ordenes.flush();
        return aDetalle(o, actor);
    }

    @Transactional
    public OrdenDetalleDto quitarRepuesto(Long id, Long lineaId, UsuarioPrincipal actor) {
        OrdenTrabajo o = bloquear(id);
        exigirAcceso(o, actor);
        exigirEditable(o);
        boolean quitada = o.getRepuestos().removeIf(d -> d.getId().equals(lineaId));
        if (!quitada) {
            throw new RecursoNoEncontradoException("Línea de repuesto", lineaId);
        }
        o.recalcularTotal();
        return aDetalle(o, actor);
    }

    // ------------------------------------------------------------ Cambio de estado

    @Transactional
    public OrdenDetalleDto cambiarEstado(Long id, CambioEstadoRequest req, UsuarioPrincipal actor) {
        OrdenTrabajo o = bloquear(id);
        exigirAcceso(o, actor);
        EstadoOrden origen = o.getEstado();
        EstadoOrden destino = req.estado();

        if (!origen.puedePasarA(destino)) {
            throw new NegocioException("No se puede pasar de " + origen + " a " + destino
                    + ". El orden es: Recibida, En diagnóstico, En reparación, Completada, Entregada.");
        }
        if (!OrdenEstadoPolicy.puedeCambiarA(actor.getRol(), destino)) {
            throw new AccesoDenegadoNegocioException("Tu rol no puede pasar una orden a " + destino);
        }
        String comentario = TextoUtil.aNullSiVacio(req.comentario());

        switch (destino) {
            case EN_DIAGNOSTICO -> {
                if (o.getMecanico() == null) {
                    throw new NegocioException("Asigna un mecánico antes de iniciar el diagnóstico");
                }
            }
            case EN_REPARACION -> {
                if (TextoUtil.aNullSiVacio(o.getDiagnostico()) == null) {
                    throw new NegocioException("Registra el diagnóstico antes de pasar a reparación");
                }
            }
            case COMPLETADA -> {
                if (TextoUtil.aNullSiVacio(o.getTrabajosRealizados()) == null) {
                    throw new NegocioException("Registra los trabajos realizados antes de completar la orden");
                }
                if (o.getServicios().isEmpty() && o.getRepuestos().isEmpty() && o.getManoObra().signum() == 0) {
                    throw new NegocioException("La orden no tiene servicios, repuestos ni mano de obra");
                }
                descontarInventario(o, actor);
                o.setFechaCierre(Tiempo.ahora());
            }
            case ENTREGADA -> {
                if (!comprobantes.existsByOrdenIdAndEstado(o.getId(), EstadoComprobante.EMITIDO)) {
                    throw new NegocioException("Emite el comprobante antes de entregar el vehículo");
                }
            }
            case CANCELADA -> {
                if (comentario == null) {
                    throw new ValidacionException("comentario", "Indica el motivo de la cancelación");
                }
                if (comprobantes.existsByOrdenIdAndEstado(o.getId(), EstadoComprobante.EMITIDO)) {
                    throw new NegocioException("Anula primero el comprobante emitido de esta orden");
                }
                if (origen == EstadoOrden.COMPLETADA) {
                    revertirInventario(o, actor);
                }
            }
            default -> throw new NegocioException("Estado no permitido");
        }
        o.setEstado(destino);
        registrarHistorial(o, origen, destino, actor, comentario);
        return aDetalle(o, actor);
    }

    // ---------------------------------------------------------------- Internos

    /** Descuenta del inventario cada línea que todavía no se descontó. Se ejecuta una sola vez por línea. */
    private void descontarInventario(OrdenTrabajo o, UsuarioPrincipal actor) {
        List<DetalleOrdenRepuesto> pendientes = o.getRepuestos().stream().filter(d -> !d.isDescontado()).toList();
        if (pendientes.isEmpty()) {
            return;
        }
        Map<Long, Repuesto> bloqueados = bloquearRepuestos(pendientes);
        Usuario u = usuarios.getReferenceById(actor.getId());
        pendientes.stream().sorted(Comparator.comparing(d -> d.getRepuesto().getId())).forEach(d -> {
            Repuesto r = bloqueados.get(d.getRepuesto().getId());
            inventario.salidaPorOrden(r, d.getCantidad(), o.getId(), claveLinea(o, d), u,
                    "Salida por orden " + o.getCodigo());
            d.setDescontado(true);
        });
    }

    private void revertirInventario(OrdenTrabajo o, UsuarioPrincipal actor) {
        List<DetalleOrdenRepuesto> descontadas = o.getRepuestos().stream().filter(DetalleOrdenRepuesto::isDescontado).toList();
        if (descontadas.isEmpty()) {
            return;
        }
        Map<Long, Repuesto> bloqueados = bloquearRepuestos(descontadas);
        Usuario u = usuarios.getReferenceById(actor.getId());
        for (DetalleOrdenRepuesto d : descontadas) {
            inventario.devolucionPorOrden(bloqueados.get(d.getRepuesto().getId()), d.getCantidad(), o.getId(),
                    claveLinea(o, d) + "-REV", u, "Devolución por cancelación de " + o.getCodigo());
            d.setDescontado(false);
        }
    }

    private Map<Long, Repuesto> bloquearRepuestos(List<DetalleOrdenRepuesto> lineas) {
        List<Long> ids = lineas.stream().<Long>map(d -> d.getRepuesto().getId()).distinct().sorted().toList();
        return repuestos.findAllByIdForUpdate(ids).stream().collect(Collectors.toMap(Repuesto::getId, Function.identity()));
    }

    private String claveLinea(OrdenTrabajo o, DetalleOrdenRepuesto d) {
        return "OT" + o.getId() + "-L" + d.getId();
    }

    private void registrarKilometraje(OrdenTrabajo o, Vehiculo v, int km) {
        if (km < v.getKilometraje()) {
            throw new ValidacionException("kilometrajeIngreso",
                    "El kilometraje no puede ser menor al último registrado (" + v.getKilometraje() + " km)");
        }
        o.setKilometrajeIngreso(km);
        v.setKilometraje(km);
    }

    private Usuario mecanicoValido(Long id) {
        Usuario u = usuarios.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Usuario", id));
        if (u.getRol() != Rol.MECANICO || !u.isActivo()) {
            throw new ValidacionException("mecanicoId", "Elige un usuario activo con rol mecánico");
        }
        return u;
    }

    private void registrarHistorial(OrdenTrabajo o, EstadoOrden de, EstadoOrden a, UsuarioPrincipal actor, String comentario) {
        OrdenHistorial h = new OrdenHistorial();
        h.setOrden(o);
        h.setEstadoAnterior(de);
        h.setEstadoNuevo(a);
        h.setUsuario(usuarios.getReferenceById(actor.getId()));
        h.setComentario(comentario);
        historiales.save(h);
    }

    private void exigirAcceso(OrdenTrabajo o, UsuarioPrincipal actor) {
        if (actor.getRol() == Rol.MECANICO
                && (o.getMecanico() == null || !o.getMecanico().getId().equals(actor.getId()))) {
            throw new AccesoDenegadoNegocioException("Esta orden no está asignada a ti");
        }
    }

    private void exigirEditable(OrdenTrabajo o) {
        if (!o.getEstado().admiteCambiosEnDetalle()) {
            throw new NegocioException("La orden está " + o.getEstado().name().toLowerCase(Locale.ROOT) + " y ya no admite cambios");
        }
    }

    private OrdenTrabajo buscar(Long id) {
        return ordenes.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Orden de trabajo", id));
    }

    private OrdenTrabajo bloquear(Long id) {
        return ordenes.findByIdForUpdate(id).orElseThrow(() -> new RecursoNoEncontradoException("Orden de trabajo", id));
    }

    private OrdenDetalleDto aDetalle(OrdenTrabajo o, UsuarioPrincipal actor) {
        Vehiculo v = o.getVehiculo();
        CalculadoraIgv.Desglose d = CalculadoraIgv.desglosar(o.getTotal());
        Comprobante comp = comprobantes.findFirstByOrdenIdAndEstado(o.getId(), EstadoComprobante.EMITIDO).orElse(null);

        boolean asignadoAActor = o.getMecanico() != null && o.getMecanico().getId().equals(actor.getId());
        boolean puedeOperar = actor.getRol() != Rol.MECANICO || asignadoAActor;
        List<EstadoOrden> permitidas = new ArrayList<>();
        if (puedeOperar) {
            for (EstadoOrden e : o.getEstado().siguientes()) {
                if (OrdenEstadoPolicy.puedeCambiarA(actor.getRol(), e)) {
                    permitidas.add(e);
                }
            }
        }
        List<HistorialEstadoDto> historial = historiales.findByOrdenIdOrderByFechaAscIdAsc(o.getId()).stream()
                .map(h -> new HistorialEstadoDto(h.getEstadoAnterior(), h.getEstadoNuevo(),
                        h.getUsuario() == null ? null : h.getUsuario().getNombre(), h.getComentario(), h.getFecha()))
                .toList();

        return new OrdenDetalleDto(o.getId(), o.getCodigo(), o.getCliente().getId(), o.getCliente().getNombre(),
                o.getCliente().getNroDocumento(), v.getId(), v.getPlaca(), v.getMarca() + " " + v.getModelo(),
                o.getMecanico() == null ? null : o.getMecanico().getId(),
                o.getMecanico() == null ? null : o.getMecanico().getNombre(),
                o.getCitaId(), o.getFalla(), o.getDiagnostico(), o.getTrabajosRealizados(), o.getKilometrajeIngreso(),
                o.getEstado(), o.getFechaIngreso(), o.getFechaEntregaEstimada(), o.getFechaCierre(), o.getManoObra(),
                o.getDescuento(), o.getTotal(), d.baseImponible(), d.igv(),
                o.getServicios().stream().map(OperacionMapper::aDto).toList(),
                o.getRepuestos().stream().map(OperacionMapper::aDto).toList(),
                historial, comp == null ? null : comp.getId(), comp == null ? null : comp.numeroCompleto(),
                permitidas, puedeOperar && o.getEstado().admiteCambiosEnDetalle());
    }
}
