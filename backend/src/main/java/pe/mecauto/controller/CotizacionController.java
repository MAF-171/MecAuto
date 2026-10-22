package pe.mecauto.controller;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pe.mecauto.domain.EstadoCotizacion;
import pe.mecauto.dto.CotizacionDtos.CotizacionDto;
import pe.mecauto.dto.CotizacionDtos.CotizacionRequest;
import pe.mecauto.dto.OrdenDtos.OrdenDetalleDto;
import pe.mecauto.dto.PageDto;
import pe.mecauto.security.UsuarioPrincipal;
import pe.mecauto.service.CotizacionService;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/cotizaciones")
@PreAuthorize(Roles.ADMIN_RECEPCION)
public class CotizacionController {

    private final CotizacionService service;

    public CotizacionController(CotizacionService service) {
        this.service = service;
    }

    @GetMapping
    public PageDto<CotizacionDto> listar(@RequestParam(required = false) String q,
                                         @RequestParam(required = false) EstadoCotizacion estado,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                         @RequestParam(defaultValue = "0") int pagina,
                                         @RequestParam(defaultValue = "20") int tamano) {
        return service.listar(q, estado, desde, hasta, pagina, tamano);
    }

    @GetMapping("/{id}")
    public CotizacionDto obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CotizacionDto crear(@Valid @RequestBody CotizacionRequest req, @AuthenticationPrincipal UsuarioPrincipal actor) {
        return service.crear(req, actor);
    }

    @PutMapping("/{id}")
    public CotizacionDto actualizar(@PathVariable Long id, @Valid @RequestBody CotizacionRequest req) {
        return service.actualizar(id, req);
    }

    @PostMapping("/{id}/aprobar")
    public CotizacionDto aprobar(@PathVariable Long id) {
        return service.aprobar(id);
    }

    @PostMapping("/{id}/rechazar")
    public CotizacionDto rechazar(@PathVariable Long id) {
        return service.rechazar(id);
    }

    @PostMapping("/{id}/convertir")
    @ResponseStatus(HttpStatus.CREATED)
    public OrdenDetalleDto convertir(@PathVariable Long id, @AuthenticationPrincipal UsuarioPrincipal actor) {
        return service.convertirEnOrden(id, actor);
    }
}
