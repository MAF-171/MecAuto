package pe.mecauto.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.mecauto.dto.PageDto;
import pe.mecauto.dto.PersonaDtos.ClienteDto;
import pe.mecauto.dto.PersonaDtos.ClienteRequest;
import pe.mecauto.dto.PersonaDtos.VehiculoDto;
import pe.mecauto.dto.UsuarioDtos.EstadoActivoRequest;
import pe.mecauto.service.ClienteService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/clientes")
@PreAuthorize(Roles.ADMIN_RECEPCION)
public class ClienteController {

    private final ClienteService service;

    public ClienteController(ClienteService service) {
        this.service = service;
    }

    @GetMapping
    public PageDto<ClienteDto> listar(@RequestParam(required = false) String q,
                                      @RequestParam(defaultValue = "false") boolean soloActivos,
                                      @RequestParam(defaultValue = "0") int pagina,
                                      @RequestParam(defaultValue = "20") int tamano) {
        return service.listar(q, soloActivos, pagina, tamano);
    }

    @GetMapping("/{id}")
    public ClienteDto obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @GetMapping("/{id}/vehiculos")
    public List<VehiculoDto> vehiculos(@PathVariable Long id) {
        return service.vehiculosDe(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClienteDto crear(@Valid @RequestBody ClienteRequest req) {
        return service.crear(req);
    }

    @PutMapping("/{id}")
    public ClienteDto actualizar(@PathVariable Long id, @Valid @RequestBody ClienteRequest req) {
        return service.actualizar(id, req);
    }

    @PatchMapping("/{id}/estado")
    public ClienteDto estado(@PathVariable Long id, @Valid @RequestBody EstadoActivoRequest req) {
        return service.cambiarEstado(id, req.activo());
    }
}
