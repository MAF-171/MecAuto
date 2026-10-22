package pe.mecauto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de integración (H2 en memoria, NO SQL Server): comprueba que las rutas se protegen en el servidor.
 * Requiere Maven para ejecutarse: mvn test
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SeguridadEndpointsIT {

    @Autowired
    MockMvc mvc;

    @Test
    @DisplayName("Sin token las rutas internas responden 401")
    void sinTokenEs401() throws Exception {
        mvc.perform(get("/api/v1/clientes")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/dashboard")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Las rutas públicas no piden token")
    void publicasSinToken() throws Exception {
        mvc.perform(get("/api/v1/public/taller")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/public/servicios")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "MECANICO")
    @DisplayName("El mecánico no puede entrar a usuarios ni reportes (403)")
    void mecanicoNoAdministra() throws Exception {
        mvc.perform(get("/api/v1/usuarios")).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/reportes")).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/clientes")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "RECEPCION")
    @DisplayName("Recepción no puede administrar usuarios ni ver reportes (403)")
    void recepcionNoAdministra() throws Exception {
        mvc.perform(get("/api/v1/usuarios")).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/reportes")).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Login con datos vacíos responde 400 y no 200")
    void loginInvalido() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
    }
}
