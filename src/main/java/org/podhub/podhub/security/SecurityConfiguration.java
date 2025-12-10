package org.podhub.podhub.security;

import lombok.RequiredArgsConstructor;
import org.podhub.podhub.security.filter.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Configuración principal de seguridad:
 * - Rutas públicas
 * - Rutas protegidas
 * - JWT Filter
 * - CORS
 * - Stateless sessions
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {

    private final JwtAuthenticationFilter jwtFilter;
    private final AuthenticationProvider authenticationProvider;
    private final CorsConfigurationSource corsConfigurationSource;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                // Activar CORS (configurado en APIConfiguration)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))

                // Desactivar CSRF (solo útil con cookies)
                .csrf(csrf -> csrf.disable())

                // Peticiones públicas
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/login",
                                "/api/auth/register",
                                "/api/auth/refresh",
                                "/error"
                        ).permitAll()


                        // Todo lo demás → requiere JWT
                        .anyRequest().authenticated()
                )

                // No usar sesiones → JWT mode
                .sessionManagement(sess ->
                        sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Registrar nuestra AuthenticationProvider
                .authenticationProvider(authenticationProvider)

                // Añadir filtro JWT antes del filtro de autenticación por username/password
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
