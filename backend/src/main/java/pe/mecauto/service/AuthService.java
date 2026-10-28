package pe.mecauto.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import pe.mecauto.dto.AuthDtos.LoginRequest;
import pe.mecauto.dto.AuthDtos.LoginResponse;
import pe.mecauto.dto.UsuarioDtos.UsuarioDto;
import pe.mecauto.security.JwtService;
import pe.mecauto.security.LoginAttemptService;
import pe.mecauto.security.UsuarioPrincipal;

import java.util.Locale;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final AuthenticationManager authManager;
    private final JwtService jwt;
    private final LoginAttemptService intentos;

    public AuthService(AuthenticationManager authManager, JwtService jwt, LoginAttemptService intentos) {
        this.authManager = authManager;
        this.jwt = jwt;
        this.intentos = intentos;
    }

    public LoginResponse login(LoginRequest req) {
        String username = req.username().trim().toLowerCase(Locale.ROOT);
        intentos.verificarNoBloqueado(username);
        try {
            Authentication auth = authManager.authenticate(new UsernamePasswordAuthenticationToken(username, req.password()));
            UsuarioPrincipal p = (UsuarioPrincipal) auth.getPrincipal();
            intentos.registrarExito(username);
            log.info("Inicio de sesión correcto: {}", username);
            return new LoginResponse(jwt.generar(p), "Bearer", jwt.getExpiracionMinutos(), aDto(p));
        } catch (AuthenticationException e) {
            intentos.registrarFallo(username);
            log.warn("Inicio de sesión fallido para el usuario '{}'", username);
            // Mismo mensaje para usuario inexistente, clave errónea o cuenta desactivada: no revela cuál fue.
            throw new BadCredentialsException("Credenciales inválidas");
        }
    }

    public UsuarioDto aDto(UsuarioPrincipal p) {
        return new UsuarioDto(p.getId(), p.getUsername(), p.getNombre(), p.getRol(), p.isEnabled(), null, null, null);
    }
}
