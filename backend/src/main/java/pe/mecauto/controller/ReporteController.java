package pe.mecauto.controller;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.mecauto.dto.ReporteDto;
import pe.mecauto.service.ExcelExporter;
import pe.mecauto.service.ReporteService;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

/** Reportes: solo administrador (indicadores e ingresos del negocio). */
@RestController
@RequestMapping("/api/v1/reportes")
@PreAuthorize(Roles.ADMIN)
public class ReporteController {

    private static final MediaType XLSX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final ReporteService service;
    private final ExcelExporter excel;

    public ReporteController(ReporteService service, ExcelExporter excel) {
        this.service = service;
        this.excel = excel;
    }

    @GetMapping
    public List<String> tipos() {
        return service.tiposDisponibles();
    }

    @GetMapping("/{tipo}")
    public ReporteDto generar(@PathVariable String tipo,
                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                              @RequestParam(required = false) String estado) {
        return service.generar(tipo, desde, hasta, estado);
    }

    @GetMapping("/{tipo}/excel")
    public ResponseEntity<byte[]> exportar(@PathVariable String tipo,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                           @RequestParam(required = false) String estado) {
        ReporteDto r = service.generar(tipo, desde, hasta, estado);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("mecauto-" + r.tipo() + ".xlsx", StandardCharsets.UTF_8).build().toString())
                .contentType(XLSX)
                .body(excel.exportar(r));
    }
}
