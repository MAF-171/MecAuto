package pe.mecauto.controller;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pe.mecauto.domain.TipoMovimiento;
import pe.mecauto.dto.CatalogoDtos.MovimientoDto;
import pe.mecauto.dto.CatalogoDtos.MovimientoRequest;
import pe.mecauto.dto.CatalogoDtos.RepuestoDto;
import pe.mecauto.dto.CatalogoDtos.RepuestoRequest;
import pe.mecauto.dto.PageDto;
import pe.mecauto.dto.UsuarioDtos.EstadoActivoRequest;
import pe.mecauto.security.UsuarioPrincipal;
import pe.mecauto.service.InventarioService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class InventarioController {

    private final InventarioService service;

    public InventarioController(InventarioService service) {
        this.service = service;
    }

    @GetMapping("/repuestos")
    @PreAuthorize(Roles.TODOS)
    public PageDto<RepuestoDto> listar(@RequestParam(required = false) String q,
                                       @RequestParam(required = false) String categoria,
                                       @RequestParam(defaultValue = "false") boolean soloBajoStock,
                                       @RequestParam(defaultValue = "false") boolean soloActivos,
                                       @RequestParam(defaultValue = "0") int pagina,
                                       @RequestParam(defaultValue = "20") int tamano) {
        return service.listar(q, categoria, soloBajoStock, soloActivos, pagina, tamano);
    }

    @GetMapping("/repuestos/stock-bajo")
    @PreAuthorize(Roles.TODOS)
    public List<RepuestoDto> stockBajo() {
        return service.stockBajo();
    }

    @GetMapping("/repuestos/{id}")
    @PreAuthorize(Roles.TODOS)
    public RepuestoDto obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @PostMapping("/repuestos")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Roles.ADMIN)
    public RepuestoDto crear(@Valid @RequestBody RepuestoRequest req, @AuthenticationPrincipal UsuarioPrincipal actor) {
        return service.crear(req, actor);
    }

    @PutMapping("/repuestos/{id}")
    @PreAuthorize(Roles.ADMIN)
    public RepuestoDto actualizar(@PathVariable Long id, @Valid @RequestBody RepuestoRequest req) {
        return service.actualizar(id, req);
    }

    @PatchMapping("/repuestos/{id}/estado")
    @PreAuthorize(Roles.ADMIN)
    public RepuestoDto estado(@PathVariable Long id, @Valid @RequestBody EstadoActivoRequest req) {
        return service.cambiarEstado(id, req.activo());
    }

    /** Entradas, salidas y ajustes. El servicio valida qué tipos puede registrar cada rol. */
    @PostMapping("/repuestos/{id}/movimientos")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Roles.TODOS)
    public MovimientoDto registrar(@PathVariable Long id, @Valid @RequestBody MovimientoRequest req,
                                   @AuthenticationPrincipal UsuarioPrincipal actor) {
        return service.registrar(id, req, actor);
    }

    @GetMapping("/movimientos")
    @PreAuthorize(Roles.ADMIN_RECEPCION)
    public PageDto<MovimientoDto> movimientos(@RequestParam(required = false) Long repuestoId,
                                              @RequestParam(required = false) TipoMovimiento tipo,
                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                              @RequestParam(defaultValue = "0") int pagina,
                                              @RequestParam(defaultValue = "20") int tamano) {
        return service.listarMovimientos(repuestoId, tipo, desde, hasta, pagina, tamano);
    }
}
