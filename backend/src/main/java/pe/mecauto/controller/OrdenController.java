package pe.mecauto.controller;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pe.mecauto.domain.EstadoOrden;
import pe.mecauto.dto.CotizacionDtos.CotizacionDto;
import pe.mecauto.dto.OrdenDtos.*;
import pe.mecauto.dto.PageDto;
import pe.mecauto.security.UsuarioPrincipal;
import pe.mecauto.service.CotizacionService;
import pe.mecauto.service.OrdenService;

import java.time.LocalDate;

/**
 * Órdenes de trabajo. El servicio aplica además la regla "el mecánico solo ve y modifica sus órdenes".
 */
@RestController
@RequestMapping("/api/v1/ordenes")
@PreAuthorize(Roles.TODOS)
public class OrdenController {

    private final OrdenService service;
    private final CotizacionService cotizaciones;

    public OrdenController(OrdenService service, CotizacionService cotizaciones) {
        this.service = service;
        this.cotizaciones = cotizaciones;
    }

    @GetMapping
    public PageDto<OrdenResumenDto> listar(@RequestParam(required = false) String q,
                                           @RequestParam(required = false) EstadoOrden estado,
                                           @RequestParam(required = false) Long mecanicoId,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                           @RequestParam(defaultValue = "0") int pagina,
                                           @RequestParam(defaultValue = "20") int tamano,
                                           @AuthenticationPrincipal UsuarioPrincipal actor) {
        return service.listar(q, estado, mecanicoId, desde, hasta, pagina, tamano, actor);
    }

    @GetMapping("/{id}")
    public OrdenDetalleDto obtener(@PathVariable Long id, @AuthenticationPrincipal UsuarioPrincipal actor) {
        return service.obtener(id, actor);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Roles.ADMIN_RECEPCION)
    public OrdenDetalleDto crear(@Valid @RequestBody OrdenCrearRequest req, @AuthenticationPrincipal UsuarioPrincipal actor) {
        return service.crear(req, actor);
    }

    @PostMapping("/desde-cita/{citaId}")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Roles.ADMIN_RECEPCION)
    public OrdenDetalleDto desdeCita(@PathVariable Long citaId, @AuthenticationPrincipal UsuarioPrincipal actor) {
        return service.crearDesdeCita(citaId, actor);
    }

    @PutMapping("/{id}")
    public OrdenDetalleDto actualizar(@PathVariable Long id, @Valid @RequestBody OrdenActualizarRequest req,
                                      @AuthenticationPrincipal UsuarioPrincipal actor) {
        return service.actualizar(id, req, actor);
    }

    @PatchMapping("/{id}/mecanico")
    @PreAuthorize(Roles.ADMIN_RECEPCION)
    public OrdenDetalleDto asignarMecanico(@PathVariable Long id, @RequestBody AsignarMecanicoRequest req,
                                           @AuthenticationPrincipal UsuarioPrincipal actor) {
        return service.asignarMecanico(id, req, actor);
    }

    @PostMapping("/{id}/estado")
    public OrdenDetalleDto cambiarEstado(@PathVariable Long id, @Valid @RequestBody CambioEstadoRequest req,
                                         @AuthenticationPrincipal UsuarioPrincipal actor) {
        return service.cambiarEstado(id, req, actor);
    }

    @PostMapping("/{id}/servicios")
    @ResponseStatus(HttpStatus.CREATED)
    public OrdenDetalleDto agregarServicio(@PathVariable Long id, @Valid @RequestBody AgregarServicioRequest req,
                                           @AuthenticationPrincipal UsuarioPrincipal actor) {
        return service.agregarServicio(id, req, actor);
    }

    @DeleteMapping("/{id}/servicios/{lineaId}")
    public OrdenDetalleDto quitarServicio(@PathVariable Long id, @PathVariable Long lineaId,
                                          @AuthenticationPrincipal UsuarioPrincipal actor) {
        return service.quitarServicio(id, lineaId, actor);
    }

    @PostMapping("/{id}/repuestos")
    @ResponseStatus(HttpStatus.CREATED)
    public OrdenDetalleDto agregarRepuesto(@PathVariable Long id, @Valid @RequestBody AgregarRepuestoRequest req,
                                           @AuthenticationPrincipal UsuarioPrincipal actor) {
        return service.agregarRepuesto(id, req, actor);
    }

    @DeleteMapping("/{id}/repuestos/{lineaId}")
    public OrdenDetalleDto quitarRepuesto(@PathVariable Long id, @PathVariable Long lineaId,
                                          @AuthenticationPrincipal UsuarioPrincipal actor) {
        return service.quitarRepuesto(id, lineaId, actor);
    }

    /** RF11: genera una cotización a partir de esta orden. */
    @PostMapping("/{id}/cotizacion")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Roles.ADMIN_RECEPCION)
    public CotizacionDto generarCotizacion(@PathVariable Long id, @AuthenticationPrincipal UsuarioPrincipal actor) {
        return cotizaciones.generarDesdeOrden(id, actor);
    }
}
