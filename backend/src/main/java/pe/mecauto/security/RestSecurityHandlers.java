package pe.mecauto.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import pe.mecauto.exception.ApiError;

import java.io.IOException;
import java.time.OffsetDateTime;

/** Respuestas JSON para 401 (sin sesión) y 403 (sin permiso) que ocurren antes de llegar a los controladores. */
@Component
public class RestSecurityHandlers implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper mapper;

    public RestSecurityHandlers(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void commence(HttpServletRequest req, HttpServletResponse res, AuthenticationException ex) throws IOException {
        escribir(req, res, HttpStatus.UNAUTHORIZED, "Debes iniciar sesión para continuar");
    }

    @Override
    public void handle(HttpServletRequest req, HttpServletResponse res, AccessDeniedException ex) throws IOException {
        escribir(req, res, HttpStatus.FORBIDDEN, "No tienes permiso para realizar esta acción");
    }

    private void escribir(HttpServletRequest req, HttpServletResponse res, HttpStatus status, String mensaje) throws IOException {
        res.setStatus(status.value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        mapper.writeValue(res.getOutputStream(),
                new ApiError(OffsetDateTime.now(), status.value(), status.getReasonPhrase(), mensaje, req.getRequestURI(), null));
    }
}
