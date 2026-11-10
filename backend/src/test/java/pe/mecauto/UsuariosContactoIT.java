package pe.mecauto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Prueba de integración (H2 en modo SQL Server): el correo y el celular de los usuarios son opcionales.
 * En SQL Server dos NULL cuentan como repetidos en una restricción UNIQUE común, así que varios usuarios
 * sin correo deben poder coexistir; los repetidos reales los rechaza el servicio con 409.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UsuariosContactoIT {

    @Autowired
    MockMvc mvc;

    private int crear(String username, String correo) throws Exception {
        String contacto = correo == null ? "" : ",\"correo\":\"" + correo + "\"";
        return mvc.perform(post("/api/v1/usuarios").contentType("application/json")
                        .content("{\"username\":\"" + username + "\",\"nombre\":\"Prueba\",\"rol\":\"MECANICO\","
                                + "\"password\":\"Clave123456\"" + contacto + "}"))
                .andReturn().getResponse().getStatus();
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("Varios usuarios sin correo ni celular se crean sin conflicto; un correo repetido da 409")
    void contactoOpcionalYUnico() throws Exception {
        assertEquals(201, crear("sincorreo1", null));
        assertEquals(201, crear("sincorreo2", null));
        assertEquals(201, crear("concorreo1", "uno@taller.pe"));
        assertEquals(409, crear("concorreo2", "UNO@taller.pe"));
    }
}
