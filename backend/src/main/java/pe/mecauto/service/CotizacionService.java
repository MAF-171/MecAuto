package pe.mecauto.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.mecauto.domain.CalculadoraIgv;
import pe.mecauto.domain.EstadoCotizacion;
import pe.mecauto.domain.TipoLineaCotizacion;
import pe.mecauto.dto.CotizacionDtos.CotizacionDto;
import pe.mecauto.dto.CotizacionDtos.CotizacionRequest;
import pe.mecauto.dto.CotizacionDtos.LineaCotizacionRequest;
import pe.mecauto.dto.OrdenDtos.OrdenDetalleDto;
import pe.mecauto.dto.PageDto;
import pe.mecauto.entity.Cliente;
import pe.mecauto.entity.Cotizacion;
import pe.mecauto.entity.DetalleCotizacion;
import pe.mecauto.entity.DetalleOrdenRepuesto;
import pe.mecauto.entity.DetalleOrdenServicio;
import pe.mecauto.entity.OrdenTrabajo;
import pe.mecauto.entity.Repuesto;
import pe.mecauto.entity.Servicio;
import pe.mecauto.entity.Vehiculo;
import pe.mecauto.exception.NegocioException;
import pe.mecauto.exception.RecursoNoEncontradoException;
import pe.mecauto.exception.ValidacionException;
import pe.mecauto.mapper.OperacionMapper;
import pe.mecauto.repository.ClienteRepository;
import pe.mecauto.repository.CotizacionRepository;
import pe.mecauto.repository.OrdenTrabajoRepository;
import pe.mecauto.repository.RepuestoRepository;
import pe.mecauto.repository.ServicioRepository;
import pe.mecauto.repository.UsuarioRepository;
import pe.mecauto.repository.VehiculoRepository;
import pe.mecauto.security.UsuarioPrincipal;
import pe.mecauto.util.TextoUtil;
import pe.mecauto.util.Tiempo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;

/**
 * Cotizaciones. Los precios incluyen IGV (RN06): el IGV se desglosa del total, nunca se suma otra vez.
 *   subtotal = suma de líneas; total = subtotal - descuento; base = total / 1.18; IGV = total - base.
 */
@Service
@Transactional(readOnly = true)
public class CotizacionService {

    private static final int VIGENCIA_POR_DEFECTO_DIAS = 15;

    private final CotizacionRepository cotizaciones;
    private final ClienteRepository clientes;
    private final VehiculoRepository vehiculos;
    private final ServicioRepository servicios;
    private final RepuestoRepository repuestos;
    private final OrdenTrabajoRepository ordenes;
    private final UsuarioRepository usuarios;
    private final SecuenciaService secuencias;
    private final OrdenService ordenService;

    public CotizacionService(CotizacionRepository cotizaciones, ClienteRepository clientes, VehiculoRepository vehiculos,
                             ServicioRepository servicios, RepuestoRepository repuestos, OrdenTrabajoRepository ordenes,
                             UsuarioRepository usuarios, SecuenciaService secuencias, OrdenService ordenService) {
        this.cotizaciones = cotizaciones;
        this.clientes = clientes;
        this.vehiculos = vehiculos;
        this.servicios = servicios;
        this.repuestos = repuestos;
        this.ordenes = ordenes;
        this.usuarios = usuarios;
        this.secuencias = secuencias;
        this.ordenService = ordenService;
    }

    // ------------------------------------------------------------------ Consultas

    public PageDto<CotizacionDto> listar(String q, EstadoCotizacion estado, LocalDate desde, LocalDate hasta,
                                         int pagina, int tamano) {
        Specification<Cotizacion> spec = (r, cq, cb) -> cb.conjunction();
        String texto = TextoUtil.limpiarBusqueda(q).toLowerCase(Locale.ROOT);
        if (!texto.isEmpty()) {
            String t = "%" + texto + "%";
            spec = spec.and((r, cq, cb) -> cb.or(
                    cb.like(cb.lower(r.get("codigo")), t),
                    cb.like(cb.lower(r.get("cliente").get("nombre")), t)));
        }
        if (estado != null) {
            spec = spec.and((r, cq, cb) -> cb.equal(r.get("estado"), estado));
        }
        if (desde != null) {
            spec = spec.and((r, cq, cb) -> cb.greaterThanOrEqualTo(r.<LocalDate>get("fechaEmision"), desde));
        }
        if (hasta != null) {
            spec = spec.and((r, cq, cb) -> cb.lessThanOrEqualTo(r.<LocalDate>get("fechaEmision"), hasta));
        }
        Page<Cotizacion> page = cotizaciones.findAll(spec,
                PageRequest.of(pagina, Math.min(tamano, 100), Sort.by(Sort.Direction.DESC, "fechaEmision", "id")));
        return PageDto.de(page, c -> OperacionMapper.aDto(c, false));
    }

    public CotizacionDto obtener(Long id) {
        return OperacionMapper.aDto(buscar(id), true);
    }

    // ------------------------------------------------------------------ Escritura

    @Transactional
    public CotizacionDto crear(CotizacionRequest req, UsuarioPrincipal actor) {
        Cotizacion c = new Cotizacion();
        c.setCodigo(secuencias.siguienteCodigo("COT"));
        c.setFechaEmision(Tiempo.hoy());
        c.setCreadoPor(usuarios.getReferenceById(actor.getId()));
        aplicar(c, req);
        return OperacionMapper.aDto(cotizaciones.save(c), true);
    }

    @Transactional
    public CotizacionDto actualizar(Long id, CotizacionRequest req) {
        Cotizacion c = buscar(id);
        if (c.getEstado() != EstadoCotizacion.PENDIENTE) {
            throw new NegocioException("Solo se puede editar una cotización pendiente");
        }
        aplicar(c, req);
        return OperacionMapper.aDto(c, true);
    }

    @Transactional
    public CotizacionDto aprobar(Long id) {
        Cotizacion c = bloquear(id);
        if (c.getEstado() != EstadoCotizacion.PENDIENTE) {
            throw new NegocioException("Solo se puede aprobar una cotización pendiente");
        }
        if (c.getVigenciaHasta().isBefore(Tiempo.hoy())) {
            throw new NegocioException("La cotización venció el " + c.getVigenciaHasta() + ". Edita la vigencia para aprobarla.");
        }
        c.setEstado(EstadoCotizacion.APROBADA);
        return OperacionMapper.aDto(c, true);
    }

    @Transactional
    public CotizacionDto rechazar(Long id) {
        Cotizacion c = bloquear(id);
        if (c.getEstado() != EstadoCotizacion.PENDIENTE) {
            throw new NegocioException("Solo se puede rechazar una cotización pendiente");
        }
        c.setEstado(EstadoCotizacion.RECHAZADA);
        return OperacionMapper.aDto(c, true);
    }

    /**
     * Convierte una cotización aprobada en una orden de trabajo. Es UNA transacción: o se crea la orden
     * y la cotización queda CONVERTIDA, o no cambia nada. El bloqueo evita convertirla dos veces.
     */
    @Transactional
    public OrdenDetalleDto convertirEnOrden(Long id, UsuarioPrincipal actor) {
        Cotizacion c = bloquear(id);
        if (c.getEstado() != EstadoCotizacion.APROBADA) {
            throw new NegocioException("Solo se puede convertir una cotización aprobada");
        }
        OrdenDetalleDto orden = ordenService.crearDesdeCotizacion(c, actor);
        c.setOrdenId(orden.id());
        c.setEstado(EstadoCotizacion.CONVERTIDA);
        return orden;
    }

    /** RF11: genera una cotización a partir de los servicios, repuestos y mano de obra de una orden existente. */
    @Transactional
    public CotizacionDto generarDesdeOrden(Long ordenId, UsuarioPrincipal actor) {
        OrdenTrabajo o = ordenes.findById(ordenId).orElseThrow(() -> new RecursoNoEncontradoException("Orden de trabajo", ordenId));
        Cotizacion c = new Cotizacion();
        c.setCodigo(secuencias.siguienteCodigo("COT"));
        c.setFechaEmision(Tiempo.hoy());
        c.setVigenciaHasta(Tiempo.hoy().plusDays(VIGENCIA_POR_DEFECTO_DIAS));
        c.setCliente(o.getCliente());
        c.setVehiculo(o.getVehiculo());
        c.setOrdenId(o.getId());
        c.setObservaciones("Generada desde la orden " + o.getCodigo());
        c.setCreadoPor(usuarios.getReferenceById(actor.getId()));
        for (DetalleOrdenServicio d : o.getServicios()) {
            c.getDetalles().add(linea(c, TipoLineaCotizacion.SERVICIO, d.getServicio(), null, d.getDescripcion(), d.getCantidad(), d.getPrecioUnitario()));
        }
        for (DetalleOrdenRepuesto d : o.getRepuestos()) {
            c.getDetalles().add(linea(c, TipoLineaCotizacion.REPUESTO, null, d.getRepuesto(),
                    d.getRepuesto().getNombre(), d.getCantidad(), d.getPrecioUnitario()));
        }
        if (o.getManoObra().signum() > 0) {
            c.getDetalles().add(linea(c, TipoLineaCotizacion.MANO_OBRA, null, null, "Mano de obra", 1, o.getManoObra()));
        }
        if (c.getDetalles().isEmpty()) {
            throw new NegocioException("La orden no tiene servicios, repuestos ni mano de obra para cotizar");
        }
        c.setDescuento(o.getDescuento());
        calcularTotales(c);
        return OperacionMapper.aDto(cotizaciones.save(c), true);
    }

    // ---------------------------------------------------------------- Internos

    private void aplicar(Cotizacion c, CotizacionRequest req) {
        Cliente cliente = clientes.findById(req.clienteId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente", req.clienteId()));
        if (!cliente.isActivo()) {
            throw new NegocioException("El cliente está desactivado");
        }
        Vehiculo vehiculo = null;
        if (req.vehiculoId() != null) {
            vehiculo = vehiculos.findById(req.vehiculoId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Vehículo", req.vehiculoId()));
            if (!vehiculo.getCliente().getId().equals(cliente.getId())) {
                throw new ValidacionException("vehiculoId", "El vehículo no pertenece al cliente seleccionado");
            }
        }
        LocalDate vigencia = req.vigenciaHasta() != null ? req.vigenciaHasta()
                : Tiempo.hoy().plusDays(VIGENCIA_POR_DEFECTO_DIAS);
        if (vigencia.isBefore(Tiempo.hoy())) {
            throw new ValidacionException("vigenciaHasta", "La vigencia no puede ser una fecha pasada");
        }
        c.setCliente(cliente);
        c.setVehiculo(vehiculo);
        c.setVigenciaHasta(vigencia);
        c.setObservaciones(TextoUtil.aNullSiVacio(req.observaciones()));
        c.setDescuento(req.descuento() == null ? BigDecimal.ZERO : CalculadoraIgv.redondear(req.descuento()));

        c.getDetalles().clear();
        for (LineaCotizacionRequest l : req.lineas()) {
            c.getDetalles().add(construirLinea(c, l));
        }
        calcularTotales(c);
    }

    private DetalleCotizacion construirLinea(Cotizacion c, LineaCotizacionRequest l) {
        TipoLineaCotizacion tipo = l.tipo();
        Servicio s = null;
        Repuesto r = null;
        String descripcion = TextoUtil.aNullSiVacio(l.descripcion());
        BigDecimal precio = l.precioUnitario();
        switch (tipo) {
            case SERVICIO -> {
                if (l.servicioId() == null) {
                    throw new ValidacionException("lineas", "Elige el servicio de cada línea de servicio");
                }
                s = servicios.findById(l.servicioId()).orElseThrow(() -> new RecursoNoEncontradoException("Servicio", l.servicioId()));
                if (descripcion == null) {
                    descripcion = s.getNombre();
                }
                if (precio == null) {
                    precio = s.getPrecioReferencial();
                }
            }
            case REPUESTO -> {
                if (l.repuestoId() == null) {
                    throw new ValidacionException("lineas", "Elige el repuesto de cada línea de repuesto");
                }
                r = repuestos.findById(l.repuestoId()).orElseThrow(() -> new RecursoNoEncontradoException("Repuesto", l.repuestoId()));
                if (descripcion == null) {
                    descripcion = r.getNombre();
                }
                if (precio == null) {
                    precio = r.getPrecioVenta();
                }
            }
            case MANO_OBRA -> {
                if (descripcion == null) {
                    descripcion = "Mano de obra";
                }
            }
        }
        if (precio == null) {
            throw new ValidacionException("lineas", "Indica el precio de \"" + descripcion + "\"");
        }
        return linea(c, tipo, s, r, descripcion, l.cantidad(), precio);
    }

    private DetalleCotizacion linea(Cotizacion c, TipoLineaCotizacion tipo, Servicio s, Repuesto r,
                                    String descripcion, int cantidad, BigDecimal precio) {
        DetalleCotizacion d = new DetalleCotizacion();
        d.setCotizacion(c);
        d.setTipo(tipo);
        d.setServicio(s);
        d.setRepuesto(r);
        d.setDescripcion(descripcion.length() > 200 ? descripcion.substring(0, 200) : descripcion);
        d.setCantidad(cantidad);
        d.setPrecioUnitario(CalculadoraIgv.redondear(precio));
        return d;
    }

    private void calcularTotales(Cotizacion c) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (DetalleCotizacion d : c.getDetalles()) {
            subtotal = subtotal.add(CalculadoraIgv.importe(d.getCantidad(), d.getPrecioUnitario()));
        }
        if (c.getDescuento().compareTo(subtotal) > 0) {
            throw new ValidacionException("descuento", "El descuento no puede superar el subtotal (" + subtotal + ")");
        }
        CalculadoraIgv.Desglose desglose = CalculadoraIgv.desglosar(subtotal.subtract(c.getDescuento()));
        c.setBaseImponible(desglose.baseImponible());
        c.setIgv(desglose.igv());
        c.setTotal(desglose.total());
    }

    private Cotizacion buscar(Long id) {
        return cotizaciones.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Cotización", id));
    }

    private Cotizacion bloquear(Long id) {
        return cotizaciones.findByIdForUpdate(id).orElseThrow(() -> new RecursoNoEncontradoException("Cotización", id));
    }
}
