package pe.mecauto.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.mecauto.dto.PageDto;
import pe.mecauto.dto.PersonaDtos.HistorialMantenimientoDto;
import pe.mecauto.dto.PersonaDtos.VehiculoDto;
import pe.mecauto.dto.PersonaDtos.VehiculoRequest;
import pe.mecauto.dto.UsuarioDtos.EstadoActivoRequest;
import pe.mecauto.service.VehiculoService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/vehiculos")
public class VehiculoController {

    private final VehiculoService service;

    public VehiculoController(VehiculoService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize(Roles.TODOS)
    public PageDto<VehiculoDto> listar(@RequestParam(required = false) String q,
                                       @RequestParam(required = false) Long clienteId,
                                       @RequestParam(defaultValue = "false") boolean soloActivos,
                                       @RequestParam(defaultValue = "0") int pagina,
                                       @RequestParam(defaultValue = "20") int tamano) {
        return service.listar(q, clienteId, soloActivos, pagina, tamano);
    }

    @GetMapping("/{id}")
    @PreAuthorize(Roles.TODOS)
    public VehiculoDto obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    /** Historial de mantenimiento (RF04): órdenes anteriores del vehículo. */
    @GetMapping("/{id}/historial")
    @PreAuthorize(Roles.TODOS)
    public List<HistorialMantenimientoDto> historial(@PathVariable Long id) {
        return service.historial(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Roles.ADMIN_RECEPCION)
    public VehiculoDto crear(@Valid @RequestBody VehiculoRequest req) {
        return service.crear(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize(Roles.ADMIN_RECEPCION)
    public VehiculoDto actualizar(@PathVariable Long id, @Valid @RequestBody VehiculoRequest req) {
        return service.actualizar(id, req);
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize(Roles.ADMIN_RECEPCION)
    public VehiculoDto estado(@PathVariable Long id, @Valid @RequestBody EstadoActivoRequest req) {
        return service.cambiarEstado(id, req.activo());
    }
}
