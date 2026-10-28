package pe.mecauto.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.mecauto.config.AppProperties;
import pe.mecauto.domain.Rol;
import pe.mecauto.entity.Usuario;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String SECRETO = "clave-solo-para-pruebas-automaticas-0123456789";

    private static JwtService servicio(String secreto, long minutos) {
        return new JwtService(new AppProperties(new AppProperties.Jwt(secreto, minutos), null, null, null, null));
    }

    private static UsuarioPrincipal ana() {
        Usuario u = new Usuario();
        u.setId(7L);
        u.setUsername("ana");
        u.setNombre("Ana");
        u.setRol(Rol.RECEPCION);
        u.setPasswordHash("hash");
        return new UsuarioPrincipal(u);
    }

    @Test
    @DisplayName("Un token generado se valida y devuelve el usuario")
    void generaYValida() {
        JwtService jwt = servicio(SECRETO, 60);
        assertEquals("ana", jwt.usuarioDelToken(jwt.generar(ana())).orElseThrow());
        assertEquals(60, jwt.getExpiracionMinutos());
    }

    @Test
    @DisplayName("Un token alterado, de otra clave o basura no es válido")
    void rechazaTokensInvalidos() {
        JwtService jwt = servicio(SECRETO, 60);
        String token = jwt.generar(ana());
        String alterado = token.substring(0, token.length() - 2) + (token.endsWith("A") ? "BB" : "AA");
        JwtService otraClave = servicio("otra-clave-distinta-para-pruebas-0123456789", 60);

        assertTrue(jwt.usuarioDelToken(alterado).isEmpty());
        assertTrue(otraClave.usuarioDelToken(token).isEmpty());
        assertTrue(jwt.usuarioDelToken("no-es-un-token").isEmpty());
        assertTrue(jwt.usuarioDelToken("").isEmpty());
    }

    @Test
    @DisplayName("Un token vencido no es válido")
    void tokenVencido() {
        JwtService jwt = servicio(SECRETO, -1);
        assertTrue(jwt.usuarioDelToken(jwt.generar(ana())).isEmpty());
    }

    @Test
    @DisplayName("El backend no arranca con un secreto ausente o corto")
    void secretoCorto() {
        assertThrows(IllegalStateException.class, () -> servicio(null, 60));
        assertThrows(IllegalStateException.class, () -> servicio("corto", 60));
    }
}
