package pe.mecauto.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Documentación interactiva en /swagger-ui.html (botón "Authorize": pega el token del login). */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI mecAutoOpenApi() {
        return new OpenAPI()
                .info(new Info().title("MecAuto API").version("1.0.0")
                        .description("API REST del sistema de gestión del Taller Mecánico MecAuto"))
                .addSecurityItem(new SecurityRequirement().addList("bearer"))
                .components(new Components().addSecuritySchemes("bearer",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")));
    }
}
