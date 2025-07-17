package com.L3Support.TicketEmailExtraction.config;

import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(
                    "http://localhost:3000",    // React default port
                    "http://localhost:3001",    // Alternative React port
                    "http://localhost:4200",    // Angular default port
                    "http://localhost:5173",    // Vite default port
                    "http://localhost:8081",    // Alternative frontend port
                    "http://127.0.0.1:3000",    // Alternative localhost
                    "http://127.0.0.1:3001",
                    "http://127.0.0.1:4200",
                    "http://127.0.0.1:5173"
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600); // Cache preflight response for 1 hour
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Allow specific origins for frontend
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:3000",    // React default port
            "http://localhost:3001",    // Alternative React port
            "http://localhost:4200",    // Angular default port
            "http://localhost:5173",    // Vite default port
            "http://localhost:8081",    // Alternative frontend port
            "http://127.0.0.1:3000",    // Alternative localhost
            "http://127.0.0.1:3001",
            "http://127.0.0.1:4200",
            "http://127.0.0.1:5173"
        ));
        
        // Allow all HTTP methods
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        
        // Allow all headers
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        // Allow credentials (cookies, authorization headers)
        configuration.setAllowCredentials(true);
        
        // Cache preflight response for 1 hour
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        
        return source;
    }
}