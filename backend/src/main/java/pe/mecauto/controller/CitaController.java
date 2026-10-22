package pe.mecauto.controller;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.mecauto.domain.EstadoCita;
import pe.mecauto.dto.CitaDtos.*;
import pe.mecauto.dto.PageDto;
import pe.mecauto.service.CitaService;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/citas")
@PreAuthorize(Roles.ADMIN_RECEPCION)
public class CitaController {

    private final CitaService service;

    public CitaController(CitaService service) {
        this.service = service;
    }

    @GetMapping
    public PageDto<CitaDto> listar(@RequestParam(required = false) String q,
                                   @RequestParam(required = false) EstadoCita estado,
                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                   @RequestParam(defaultValue = "0") int pagina,
                                   @RequestParam(defaultValue = "20") int tamano) {
        return service.listar(q, estado, desde, hasta, pagina, tamano);
    }

    @GetMapping("/disponibilidad")
    public DisponibilidadDto disponibilidad(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return service.disponibilidad(fecha);
    }

    @GetMapping("/{id}")
    public CitaDto obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CitaDto crear(@Valid @RequestBody CitaRequest req) {
        return service.crear(req);
    }

    @PutMapping("/{id}")
    public CitaDto actualizar(@PathVariable Long id, @Valid @RequestBody CitaRequest req) {
        return service.actualizar(id, req);
    }

    @PostMapping("/{id}/confirmar")
    public CitaDto confirmar(@PathVariable Long id) {
        return service.confirmar(id);
    }

    @PostMapping("/{id}/cancelar")
    public CitaDto cancelar(@PathVariable Long id) {
        return service.cancelar(id);
    }

    @PostMapping("/{id}/reprogramar")
    public CitaDto reprogramar(@PathVariable Long id, @Valid @RequestBody ReprogramarRequest req) {
        return service.reprogramar(id, req);
    }
}
