package com.educktrack.shared.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuracion de la documentacion OpenAPI / Swagger UI (RS-09, RNF-18).
 *
 * <p>Declara el esquema de seguridad JWT Bearer para que Swagger UI permita
 * probar los endpoints protegidos una vez implementado el modulo de seguridad
 * (RS-04). La UI queda accesible en {@code /swagger-ui.html}.</p>
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI educkTrackOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("EduckTrack API")
                        .description("API REST de gestion academica para instituciones educativas colombianas. "
                                + "Cada endpoint referencia el requisito funcional (RF) que implementa.")
                        .version("v0.1.0")
                        .contact(new Contact().name("EduckTrack - Universidad de Cartagena"))
                        .license(new License().name("Uso academico")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components().addSecuritySchemes(SECURITY_SCHEME_NAME,
                        new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
