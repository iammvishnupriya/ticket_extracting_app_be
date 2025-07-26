package com.L3Support.TicketEmailExtraction.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Ticket Email Extraction API")
                        .version("1.0.0")
                        .description("API for managing L3 support tickets extracted from emails"));
    }
}