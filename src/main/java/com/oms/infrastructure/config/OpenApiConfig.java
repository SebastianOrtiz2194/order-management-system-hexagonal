package com.oms.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI/Swagger configuration for API documentation.
 *
 * <p>Registers the JWT Bearer security scheme so protected endpoints can be
 * exercised directly from the Swagger UI using a token issued by
 * {@code POST /api/v1/auth/token}.</p>
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearer-jwt";

    @Bean
    public OpenAPI omsOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("Order Management System (OMS) API")
                        .description("API for OMS built with Hexagonal Architecture and Spring Boot 3")
                        .version("v1.0.0")
                        .contact(new Contact().name("Development Team").email("seb@oms.com"))
                        .license(new License().name("Apache 2.0").url("http://springdoc.org")))
                .components(new Components().addSecuritySchemes(SECURITY_SCHEME_NAME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Access token issued by POST /api/v1/auth/token")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
    }
}
