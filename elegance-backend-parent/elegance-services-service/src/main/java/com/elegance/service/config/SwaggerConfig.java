package com.elegance.service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Élégance Beauty Studio - Servicio de Catálogo & Tarifas API")
                        .version("1.0.0")
                        .description("Microservicio RESTful para la gestión del catálogo de servicios, categorías y tarifarios")
                        .contact(new Contact()
                                .name("Equipo de Desarrollo Élégance")
                                .email("soporte@elegance.com")));
    }
}
