package pe.mecauto.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;
import pe.mecauto.config.AppProperties;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

/** Crea y valida los tokens JWT (firmados con HS256). El secreto llega por variable de entorno. */
@Service
public class JwtService {

    private final SecretKey clave;
    private final long expiracionMinutos;

    public JwtService(AppProperties props) {
        String secreto = props.jwt().secret();
        if (secreto == null || secreto.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "MECAUTO_JWT_SECRET debe tener al menos 32 caracteres. Define la variable de entorno antes de iniciar el backend.");
        }
        this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        this.expiracionMinutos = props.jwt().expiracionMinutos();
    }

    public long getExpiracionMinutos() {
        return expiracionMinutos;
    }

    public String generar(UsuarioPrincipal usuario) {
        Date ahora = new Date();
        Date vence = new Date(ahora.getTime() + expiracionMinutos * 60_000L);
        return Jwts.builder()
                .subject(usuario.getUsername())
                .claim("uid", usuario.getId())
                .claim("rol", usuario.getRol().name())
                .issuedAt(ahora)
                .expiration(vence)
                .signWith(clave)
                .compact();
    }

    /** Devuelve el nombre de usuario si el token es válido y no ha vencido; vacío en otro caso. */
    public Optional<String> usuarioDelToken(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(clave).build().parseSignedClaims(token).getPayload();
            return Optional.ofNullable(claims.getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
