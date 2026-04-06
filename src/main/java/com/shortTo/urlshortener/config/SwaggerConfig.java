package com.shortTo.urlshortener.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ShortTo - URL Shortener API")
                        .description("A scalable URL shortener with analytics, caching and rate limiting")
                        .version("v1.0")
                        .contact(new Contact()
                                .name("Gaurav Jikar")
                                .email("gaurav@email.com")));
    }
}