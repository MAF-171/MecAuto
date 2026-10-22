package pe.mecauto.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.mecauto.dto.DashboardDtos.DashboardDto;
import pe.mecauto.security.UsuarioPrincipal;
import pe.mecauto.service.DashboardService;

@RestController
@RequestMapping("/api/v1/dashboard")
@PreAuthorize(Roles.TODOS)
public class DashboardController {

    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @GetMapping
    public DashboardDto obtener(@AuthenticationPrincipal UsuarioPrincipal actor) {
        return service.obtener(actor);
    }
}
