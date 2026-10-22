package pe.mecauto.service;

import com.google.common.base.Preconditions;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.mecauto.domain.Rol;
import pe.mecauto.domain.TipoMovimiento;
import pe.mecauto.dto.CatalogoDtos.MovimientoDto;
import pe.mecauto.dto.CatalogoDtos.MovimientoRequest;
import pe.mecauto.dto.CatalogoDtos.RepuestoDto;
import pe.mecauto.dto.CatalogoDtos.RepuestoRequest;
import pe.mecauto.dto.PageDto;
import pe.mecauto.entity.MovimientoInventario;
import pe.mecauto.entity.Proveedor;
import pe.mecauto.entity.Repuesto;
import pe.mecauto.entity.Usuario;
import pe.mecauto.exception.AccesoDenegadoNegocioException;
import pe.mecauto.exception.ConflictoException;
import pe.mecauto.exception.NegocioException;
import pe.mecauto.exception.RecursoNoEncontradoException;
import pe.mecauto.exception.ValidacionException;
import pe.mecauto.mapper.CatalogoMapper;
import pe.mecauto.repository.MovimientoInventarioRepository;
import pe.mecauto.repository.ProveedorRepository;
import pe.mecauto.repository.RepuestoRepository;
import pe.mecauto.repository.UsuarioRepository;
import pe.mecauto.security.UsuarioPrincipal;
import pe.mecauto.util.TextoUtil;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

/**
 * Inventario de repuestos. Garantías:
 * - el stock nunca queda negativo (RN03): se bloquea la fila del repuesto, se valida y además la BD tiene un CHECK;
 * - una misma operación no se registra dos veces: claveIdempotencia única en movimiento_inventario;
 * - cada cambio de stock deja un movimiento con el stock resultante (historial).
 */
@Service
@Transactional(readOnly = true)
public class InventarioService {

    private final RepuestoRepository repuestos;
    private final MovimientoInventarioRepository movimientos;
    private final ProveedorRepository proveedores;
    private final UsuarioRepository usuarios;
    private final ApplicationEventPublisher eventos;

    public InventarioService(RepuestoRepository repuestos, MovimientoInventarioRepository movimientos,
                             ProveedorRepository proveedores, UsuarioRepository usuarios,
                             ApplicationEventPublisher eventos) {
        this.repuestos = repuestos;
        this.movimientos = movimientos;
        this.proveedores = proveedores;
        this.usuarios = usuarios;
        this.eventos = eventos;
    }

    // ---------------------------------------------------------------- Repuestos

    public PageDto<RepuestoDto> listar(String q, String categoria, boolean soloBajoStock, boolean soloActivos,
                                       int pagina, int tamano) {
        String t = "%" + TextoUtil.limpiarBusqueda(q).toLowerCase(Locale.ROOT) + "%";
        Specification<Repuesto> spec = (r, cq, cb) -> cb.or(
                cb.like(cb.lower(r.get("codigo")), t), cb.like(cb.lower(r.get("nombre")), t));
        String cat = TextoUtil.aNullSiVacio(categoria);
        if (cat != null) {
            spec = spec.and((r, cq, cb) -> cb.equal(cb.lower(r.get("categoria")), cat.toLowerCase(Locale.ROOT)));
        }
        if (soloBajoStock) {
            spec = spec.and((r, cq, cb) -> cb.le(r.<Integer>get("stockActual"), r.<Integer>get("stockMinimo")));
        }
        if (soloActivos) {
            spec = spec.and((r, cq, cb) -> cb.isTrue(r.get("activo")));
        }
        Page<Repuesto> page = repuestos.findAll(spec, PageRequest.of(pagina, Math.min(tamano, 100), Sort.by("nombre")));
        return PageDto.de(page, CatalogoMapper::aDto);
    }

    public List<RepuestoDto> stockBajo() {
        return repuestos.listarStockBajo().stream().map(CatalogoMapper::aDto).toList();
    }

    public RepuestoDto obtener(Long id) {
        return CatalogoMapper.aDto(buscar(id));
    }

    @Transactional
    public RepuestoDto crear(RepuestoRequest req, UsuarioPrincipal actor) {
        String codigo = req.codigo().trim().toUpperCase(Locale.ROOT);
        if (repuestos.existsByCodigo(codigo)) {
            throw new ConflictoException("Ya existe un repuesto con el código " + codigo);
        }
        Repuesto r = new Repuesto();
        r.setStockActual(0);
        aplicar(r, req, codigo);
        repuestos.saveAndFlush(r);
        int inicial = req.stockInicial() == null ? 0 : req.stockInicial();
        if (inicial > 0) {
            aplicarMovimiento(r, TipoMovimiento.ENTRADA, inicial, "Stock inicial", null, usuario(actor), null);
        }
        return CatalogoMapper.aDto(r);
    }

    /** Edita los datos del repuesto. El stock NO se cambia aquí: solo mediante movimientos. */
    @Transactional
    public RepuestoDto actualizar(Long id, RepuestoRequest req) {
        Repuesto r = buscar(id);
        String codigo = req.codigo().trim().toUpperCase(Locale.ROOT);
        if (!r.getCodigo().equals(codigo) && repuestos.existsByCodigo(codigo)) {
            throw new ConflictoException("Ya existe un repuesto con el código " + codigo);
        }
        aplicar(r, req, codigo);
        return CatalogoMapper.aDto(r);
    }

    @Transactional
    public RepuestoDto cambiarEstado(Long id, boolean activo) {
        Repuesto r = buscar(id);
        r.setActivo(activo);
        return CatalogoMapper.aDto(r);
    }

    // -------------------------------------------------------------- Movimientos

    public PageDto<MovimientoDto> listarMovimientos(Long repuestoId, TipoMovimiento tipo, LocalDate desde,
                                                    LocalDate hasta, int pagina, int tamano) {
        Specification<MovimientoInventario> spec = (r, cq, cb) -> cb.conjunction();
        if (repuestoId != null) {
            spec = spec.and((r, cq, cb) -> cb.equal(r.get("repuesto").get("id"), repuestoId));
        }
        if (tipo != null) {
            spec = spec.and((r, cq, cb) -> cb.equal(r.get("tipo"), tipo));
        }
        if (desde != null) {
            spec = spec.and((r, cq, cb) -> cb.greaterThanOrEqualTo(r.<java.time.LocalDateTime>get("fecha"), desde.atStartOfDay()));
        }
        if (hasta != null) {
            spec = spec.and((r, cq, cb) -> cb.lessThan(r.<java.time.LocalDateTime>get("fecha"), hasta.plusDays(1).atStartOfDay()));
        }
        Page<MovimientoInventario> page = movimientos.findAll(spec,
                PageRequest.of(pagina, Math.min(tamano, 100), Sort.by(Sort.Direction.DESC, "fecha", "id")));
        return PageDto.de(page, CatalogoMapper::aDto);
    }

    /** Registro manual (pantalla de inventario). Reglas por rol: mecánico solo salidas; recepción no ajusta. */
    @Transactional
    public MovimientoDto registrar(Long repuestoId, MovimientoRequest req, UsuarioPrincipal actor) {
        validarPermisoMovimiento(actor.getRol(), req.tipo());
        String clave = TextoUtil.aNullSiVacio(req.claveIdempotencia());
        if (clave != null) {
            var existente = movimientos.findByClaveIdempotencia(clave);
            if (existente.isPresent()) {
                if (!existente.get().getRepuesto().getId().equals(repuestoId)) {
                    throw new ConflictoException("La clave de idempotencia ya se usó en otro repuesto");
                }
                return CatalogoMapper.aDto(existente.get()); // misma solicitud reenviada: no se repite
            }
        }
        Repuesto r = repuestos.findByIdForUpdate(repuestoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Repuesto", repuestoId));
        if (!r.isActivo()) {
            throw new NegocioException("El repuesto está desactivado");
        }
        int cantidad = req.cantidad();
        String motivo = TextoUtil.aNullSiVacio(req.motivo());
        if (req.tipo() == TipoMovimiento.AJUSTE && motivo == null) {
            throw new ValidacionException("motivo", "El motivo es obligatorio en un ajuste de inventario");
        }
        if (req.tipo() != TipoMovimiento.AJUSTE && cantidad <= 0) {
            throw new ValidacionException("cantidad", "La cantidad debe ser mayor que 0");
        }
        MovimientoInventario m = aplicarMovimiento(r, req.tipo(), cantidad, motivo, null, usuario(actor), clave);
        return CatalogoMapper.aDto(m);
    }

    /**
     * Salida de inventario originada en una orden de trabajo. El repuesto DEBE venir bloqueado (findByIdForUpdate).
     * La clave identifica la línea de la orden: si ya existe un movimiento con esa clave no se vuelve a descontar.
     */
    @Transactional
    public void salidaPorOrden(Repuesto bloqueado, int cantidad, Long ordenId, String clave, Usuario usuario, String motivo) {
        Preconditions.checkNotNull(bloqueado, "El repuesto debe venir bloqueado (findByIdForUpdate)");
        Preconditions.checkArgument(cantidad > 0, "La cantidad de una salida por orden debe ser mayor que cero: %s", cantidad);
        if (movimientos.existsByClaveIdempotencia(clave)) {
            return;
        }
        aplicarMovimiento(bloqueado, TipoMovimiento.SALIDA, cantidad, motivo, ordenId, usuario, clave);
    }

    /** Devuelve al inventario lo descontado por una orden que se cancela. */
    @Transactional
    public void devolucionPorOrden(Repuesto bloqueado, int cantidad, Long ordenId, String clave, Usuario usuario, String motivo) {
        Preconditions.checkNotNull(bloqueado, "El repuesto debe venir bloqueado (findByIdForUpdate)");
        Preconditions.checkArgument(cantidad > 0, "La cantidad de una devolución por orden debe ser mayor que cero: %s", cantidad);
        if (movimientos.existsByClaveIdempotencia(clave)) {
            return;
        }
        aplicarMovimiento(bloqueado, TipoMovimiento.ENTRADA, cantidad, motivo, ordenId, usuario, clave);
    }

    // ---------------------------------------------------------------- Internos

    /**
     * Único punto donde cambia el stock. cantidad: unidades (ENTRADA/SALIDA) o stock contado (AJUSTE).
     */
    private MovimientoInventario aplicarMovimiento(Repuesto r, TipoMovimiento tipo, int cantidad, String motivo,
                                                   Long ordenId, Usuario usuario, String clave) {
        int delta;
        switch (tipo) {
            case ENTRADA -> delta = cantidad;
            case SALIDA -> {
                if (r.getStockActual() < cantidad) {
                    throw new NegocioException("Stock insuficiente de \"" + r.getNombre() + "\": disponible "
                            + r.getStockActual() + ", solicitado " + cantidad);
                }
                delta = -cantidad;
            }
            case AJUSTE -> {
                if (cantidad < 0) {
                    throw new ValidacionException("cantidad", "El stock contado no puede ser negativo");
                }
                delta = cantidad - r.getStockActual();
                if (delta == 0) {
                    throw new NegocioException("El stock contado coincide con el stock actual; no hay nada que ajustar");
                }
            }
            default -> throw new IllegalStateException("Tipo de movimiento no soportado: " + tipo);
        }
        int nuevoStock = r.getStockActual() + delta;
        if (nuevoStock < 0) { // red de seguridad adicional (RN03)
            throw new NegocioException("El stock no puede quedar negativo");
        }
        r.setStockActual(nuevoStock);

        MovimientoInventario m = new MovimientoInventario();
        m.setRepuesto(r);
        m.setTipo(tipo);
        m.setCantidad(delta);
        m.setStockResultante(nuevoStock);
        m.setMotivo(motivo);
        m.setOrdenId(ordenId);
        m.setUsuario(usuario);
        m.setClaveIdempotencia(clave);
        movimientos.save(m);

        if (delta < 0 && r.estaBajoStock()) {
            eventos.publishEvent(new StockBajoEvent(r.getId(), r.getCodigo(), r.getNombre(), nuevoStock, r.getStockMinimo()));
        }
        return m;
    }

    private void validarPermisoMovimiento(Rol rol, TipoMovimiento tipo) {
        boolean permitido = switch (rol) {
            case ADMINISTRADOR -> true;
            case RECEPCION -> tipo != TipoMovimiento.AJUSTE;
            case MECANICO -> tipo == TipoMovimiento.SALIDA;
        };
        if (!permitido) {
            throw new AccesoDenegadoNegocioException("Tu rol no puede registrar este tipo de movimiento");
        }
    }

    private void aplicar(Repuesto r, RepuestoRequest req, String codigo) {
        r.setCodigo(codigo);
        r.setNombre(req.nombre().trim());
        r.setCategoria(TextoUtil.aNullSiVacio(req.categoria()));
        r.setPrecioCompra(req.precioCompra());
        r.setPrecioVenta(req.precioVenta());
        r.setStockMinimo(req.stockMinimo());
        if (req.proveedorId() == null) {
            r.setProveedor(null);
        } else {
            Proveedor p = proveedores.findById(req.proveedorId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Proveedor", req.proveedorId()));
            r.setProveedor(p);
        }
    }

    private Usuario usuario(UsuarioPrincipal actor) {
        return actor == null ? null : usuarios.getReferenceById(actor.getId());
    }

    private Repuesto buscar(Long id) {
        return repuestos.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Repuesto", id));
    }
}
