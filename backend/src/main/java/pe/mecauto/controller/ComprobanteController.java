package pe.mecauto.controller;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pe.mecauto.domain.EstadoComprobante;
import pe.mecauto.domain.TipoComprobante;
import pe.mecauto.dto.ComprobanteDtos.AnularComprobanteRequest;
import pe.mecauto.dto.ComprobanteDtos.ComprobanteDto;
import pe.mecauto.dto.ComprobanteDtos.EmitirComprobanteRequest;
import pe.mecauto.dto.PageDto;
import pe.mecauto.security.UsuarioPrincipal;
import pe.mecauto.service.ComprobanteService;

import java.time.LocalDate;

/** Comprobantes internos (no son comprobantes electrónicos de SUNAT). */
@RestController
@RequestMapping("/api/v1/comprobantes")
@PreAuthorize(Roles.ADMIN_RECEPCION)
public class ComprobanteController {

    private final ComprobanteService service;

    public ComprobanteController(ComprobanteService service) {
        this.service = service;
    }

    @GetMapping
    public PageDto<ComprobanteDto> listar(@RequestParam(required = false) String q,
                                          @RequestParam(required = false) TipoComprobante tipo,
                                          @RequestParam(required = false) EstadoComprobante estado,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                          @RequestParam(defaultValue = "0") int pagina,
                                          @RequestParam(defaultValue = "20") int tamano) {
        return service.listar(q, tipo, estado, desde, hasta, pagina, tamano);
    }

    @GetMapping("/{id}")
    public ComprobanteDto obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ComprobanteDto emitir(@Valid @RequestBody EmitirComprobanteRequest req, @AuthenticationPrincipal UsuarioPrincipal actor) {
        return service.emitir(req, actor);
    }

    @PostMapping("/{id}/anular")
    @PreAuthorize(Roles.ADMIN)
    public ComprobanteDto anular(@PathVariable Long id, @Valid @RequestBody AnularComprobanteRequest req) {
        return service.anular(id, req);
    }
}
