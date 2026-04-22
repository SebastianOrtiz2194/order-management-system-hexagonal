package com.oms.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI/Swagger configuration for API documentation.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI omsOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("Order Management System (OMS) API")
                        .description("API for OMS built with Hexagonal Architecture and Spring Boot 3")
                        .version("v1.0.0")
                        .contact(new Contact().name("Development Team").email("seb@oms.com"))
                        .license(new License().name("Apache 2.0").url("http://springdoc.org")));
    }
}
