package pe.mecauto.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.mecauto.service.ConfiguracionService;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/configuracion")
public class ConfiguracionController {

    private final ConfiguracionService service;

    public ConfiguracionController(ConfiguracionService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize(Roles.ADMIN_RECEPCION)
    public Map<String, String> obtener() {
        return service.obtenerTodo();
    }

    @PutMapping
    @PreAuthorize(Roles.ADMIN)
    public Map<String, String> actualizar(@RequestBody Map<String, String> cambios) {
        return service.actualizar(cambios);
    }
}
