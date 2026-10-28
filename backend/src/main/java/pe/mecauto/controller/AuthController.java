package pe.mecauto.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pe.mecauto.dto.AuthDtos.LoginRequest;
import pe.mecauto.dto.AuthDtos.LoginResponse;
import pe.mecauto.dto.AuthDtos.RecuperarRequest;
import pe.mecauto.dto.AuthDtos.RestablecerRequest;
import pe.mecauto.dto.UsuarioDtos.UsuarioDto;
import pe.mecauto.security.UsuarioPrincipal;
import pe.mecauto.service.AuthService;
import pe.mecauto.service.LimitadorSolicitudes;
import pe.mecauto.service.RecuperacionPasswordService;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService auth;
    private final RecuperacionPasswordService recuperacion;
    private final LimitadorSolicitudes limitador;

    public AuthController(AuthService auth, RecuperacionPasswordService recuperacion, LimitadorSolicitudes limitador) {
        this.auth = auth;
        this.recuperacion = recuperacion;
        this.limitador = limitador;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        return auth.login(req);
    }

    /** Envía un código al correo o celular. Responde igual exista o no la cuenta. Máximo 5 cada 15 minutos por IP. */
    @PostMapping("/recuperar/solicitar")
    public Map<String, String> solicitarCodigo(@Valid @RequestBody RecuperarRequest req, HttpServletRequest http) {
        limitador.verificar("recuperar:" + http.getRemoteAddr(), 5, Duration.ofMinutes(15));
        recuperacion.solicitar(req.identificador());
        return Map.of("mensaje", "Si el dato pertenece a un usuario activo, enviamos un código de 6 dígitos.");
    }

    /** Cambia la contraseña con el código recibido. Máximo 10 intentos cada 15 minutos por IP. */
    @PostMapping("/recuperar/restablecer")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void restablecer(@Valid @RequestBody RestablecerRequest req, HttpServletRequest http) {
        limitador.verificar("restablecer:" + http.getRemoteAddr(), 10, Duration.ofMinutes(15));
        recuperacion.restablecer(req.identificador(), req.codigo(), req.nuevaPassword());
    }

    @GetMapping("/me")
    public UsuarioDto me(@AuthenticationPrincipal UsuarioPrincipal actor) {
        return auth.aDto(actor);
    }
}
