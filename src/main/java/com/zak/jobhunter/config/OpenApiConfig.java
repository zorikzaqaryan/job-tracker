package com.zak.jobhunter.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Job Hunter API")
                        .version("1.0.0")
                        .description("Personal job-search automation backend. Reads Telegram channels, filters, deduplicates, and forwards matched job posts.")
                        .contact(new Contact().name("Zak")))
                .servers(List.of(new Server().url("http://localhost:8080").description("Local development")));
    }
}
