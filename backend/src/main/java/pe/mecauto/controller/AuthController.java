package pe.mecauto.controller;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.mecauto.dto.AuthDtos.LoginRequest;
import pe.mecauto.dto.AuthDtos.LoginResponse;
import pe.mecauto.dto.UsuarioDtos.UsuarioDto;
import pe.mecauto.security.UsuarioPrincipal;
import pe.mecauto.service.AuthService;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        return auth.login(req);
    }

    @GetMapping("/me")
    public UsuarioDto me(@AuthenticationPrincipal UsuarioPrincipal actor) {
        return auth.aDto(actor);
    }
}
