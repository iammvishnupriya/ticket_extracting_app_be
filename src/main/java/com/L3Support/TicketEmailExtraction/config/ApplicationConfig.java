    package com.L3Support.TicketEmailExtraction.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties
public class ApplicationConfig {
    // Configuration for file-based processing only
    // No mail dependencies required
}