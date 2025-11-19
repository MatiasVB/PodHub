package org.podhub.podhub.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Configuración de la API:
 * - Define CORS para permitir que el frontend consuma la API.
 * - Esta config será usada por Spring Security (SecurityFilterChain) más adelante.
 */
@Configuration
public class APIConfiguration {

    @Value("${cors.allowed-origins:http://localhost:3000}")
    private String allowedOrigins;

    @Value("${cors.allowed-methods:GET,POST,PUT,DELETE,PATCH,OPTIONS}")
    private String allowedMethods;

    @Value("${cors.allowed-headers:*}")
    private String allowedHeaders;

    @Value("${cors.allow-credentials:true}")
    private boolean allowCredentials;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // orígenes permitidos (puede venir como lista separada por comas en application.properties)
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .toList();
        config.setAllowedOrigins(origins);

        // métodos permitidos
        List<String> methods = Arrays.stream(allowedMethods.split(","))
                .map(String::trim)
                .toList();
        config.setAllowedMethods(methods);

        // headers permitidos
        List<String> headers = Arrays.stream(allowedHeaders.split(","))
                .map(String::trim)
                .toList();
        config.setAllowedHeaders(headers);

        config.setAllowCredentials(allowCredentials);

        // Muy importante para pasar Authorization: Bearer ...
        config.setExposedHeaders(List.of("Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Aplica esta config a todos los endpoints de la API
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
