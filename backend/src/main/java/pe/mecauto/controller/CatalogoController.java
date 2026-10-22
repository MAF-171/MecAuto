package pe.mecauto.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.mecauto.dto.CatalogoDtos.ProveedorDto;
import pe.mecauto.dto.CatalogoDtos.ProveedorRequest;
import pe.mecauto.dto.CatalogoDtos.ServicioDto;
import pe.mecauto.dto.CatalogoDtos.ServicioRequest;
import pe.mecauto.dto.UsuarioDtos.EstadoActivoRequest;
import pe.mecauto.service.ProveedorService;
import pe.mecauto.service.ServicioService;

import java.util.List;

/** Catálogo de servicios y proveedores. */
@RestController
@RequestMapping("/api/v1")
public class CatalogoController {

    private final ServicioService servicios;
    private final ProveedorService proveedores;

    public CatalogoController(ServicioService servicios, ProveedorService proveedores) {
        this.servicios = servicios;
        this.proveedores = proveedores;
    }

    // ---- Servicios (lectura para todos los roles, escritura solo administrador)

    @GetMapping("/servicios")
    @PreAuthorize(Roles.TODOS)
    public List<ServicioDto> listarServicios(@RequestParam(required = false) String q,
                                             @RequestParam(defaultValue = "false") boolean soloActivos) {
        return servicios.listar(q, soloActivos);
    }

    @GetMapping("/servicios/{id}")
    @PreAuthorize(Roles.TODOS)
    public ServicioDto obtenerServicio(@PathVariable Long id) {
        return servicios.obtener(id);
    }

    @PostMapping("/servicios")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Roles.ADMIN)
    public ServicioDto crearServicio(@Valid @RequestBody ServicioRequest req) {
        return servicios.crear(req);
    }

    @PutMapping("/servicios/{id}")
    @PreAuthorize(Roles.ADMIN)
    public ServicioDto actualizarServicio(@PathVariable Long id, @Valid @RequestBody ServicioRequest req) {
        return servicios.actualizar(id, req);
    }

    @PatchMapping("/servicios/{id}/estado")
    @PreAuthorize(Roles.ADMIN)
    public ServicioDto estadoServicio(@PathVariable Long id, @Valid @RequestBody EstadoActivoRequest req) {
        return servicios.cambiarEstado(id, req.activo());
    }

    // ---- Proveedores

    @GetMapping("/proveedores")
    @PreAuthorize(Roles.ADMIN_RECEPCION)
    public List<ProveedorDto> listarProveedores(@RequestParam(required = false) String q,
                                                @RequestParam(defaultValue = "false") boolean soloActivos) {
        return proveedores.listar(q, soloActivos);
    }

    @PostMapping("/proveedores")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Roles.ADMIN)
    public ProveedorDto crearProveedor(@Valid @RequestBody ProveedorRequest req) {
        return proveedores.crear(req);
    }

    @PutMapping("/proveedores/{id}")
    @PreAuthorize(Roles.ADMIN)
    public ProveedorDto actualizarProveedor(@PathVariable Long id, @Valid @RequestBody ProveedorRequest req) {
        return proveedores.actualizar(id, req);
    }

    @PatchMapping("/proveedores/{id}/estado")
    @PreAuthorize(Roles.ADMIN)
    public ProveedorDto estadoProveedor(@PathVariable Long id, @Valid @RequestBody EstadoActivoRequest req) {
        return proveedores.cambiarEstado(id, req.activo());
    }
}
