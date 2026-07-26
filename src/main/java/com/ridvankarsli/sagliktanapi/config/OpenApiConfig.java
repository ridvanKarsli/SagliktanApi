package com.ridvankarsli.sagliktanapi.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Rapor adım 11: Swagger/OpenAPI entegrasyonu. API JWT ile korunduğu için
// Swagger UI'da bir "Authorize" kilidi tanımlıyoruz — böylece login'den
// aldığın access token'ı buraya yapıştırıp korumalı endpoint'leri de
// doğrudan Swagger UI üzerinden deneyebiliyorsun.
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI sagliktanOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Sağlıktan API")
                        .description("Kronik ve nadir hastalıklara sahip bireylerin bir araya gelip "
                                + "bilgi paylaşabildiği sağlık odaklı sosyal platformun REST API'si.")
                        .version("v0.0.1")
                        .contact(new Contact().name("Rıdvan Karslı")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME_NAME, new SecurityScheme()
                                .name(BEARER_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
