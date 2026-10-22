package pe.mecauto.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pe.mecauto.dto.PageDto;
import pe.mecauto.dto.UsuarioDtos.*;
import pe.mecauto.security.UsuarioPrincipal;
import pe.mecauto.service.UsuarioService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/usuarios")
public class UsuarioController {

    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize(Roles.ADMIN)
    public PageDto<UsuarioDto> listar(@RequestParam(required = false) String q,
                                      @RequestParam(defaultValue = "0") int pagina,
                                      @RequestParam(defaultValue = "20") int tamano) {
        return service.listar(q, pagina, tamano);
    }

    /** Para asignar mecánicos a las órdenes. */
    @GetMapping("/mecanicos")
    @PreAuthorize(Roles.ADMIN_RECEPCION)
    public List<UsuarioDto> mecanicos() {
        return service.mecanicosActivos();
    }

    @GetMapping("/{id}")
    @PreAuthorize(Roles.ADMIN)
    public UsuarioDto obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Roles.ADMIN)
    public UsuarioDto crear(@Valid @RequestBody UsuarioCrearRequest req) {
        return service.crear(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize(Roles.ADMIN)
    public UsuarioDto actualizar(@PathVariable Long id, @Valid @RequestBody UsuarioActualizarRequest req,
                                 @AuthenticationPrincipal UsuarioPrincipal actor) {
        return service.actualizar(id, req, actor);
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize(Roles.ADMIN)
    public UsuarioDto estado(@PathVariable Long id, @Valid @RequestBody EstadoActivoRequest req,
                             @AuthenticationPrincipal UsuarioPrincipal actor) {
        return service.cambiarEstado(id, req.activo(), actor);
    }

    @PostMapping("/{id}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(Roles.ADMIN)
    public void password(@PathVariable Long id, @Valid @RequestBody CambiarPasswordRequest req) {
        service.cambiarPassword(id, req.nuevaPassword());
    }
}
