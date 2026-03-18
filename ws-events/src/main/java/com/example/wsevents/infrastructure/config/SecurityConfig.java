package com.example.wsevents.infrastructure.config;

import com.example.wsevents.infrastructure.security.JwtAuthenticationFilter;
import com.example.wsevents.infrastructure.security.JwtService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuration Spring Security.
 *
 * Règles :
 *  - POST /api/auth/token  → public (génération de token)
 *  - GET  /h2-console/**   → public (dev only)
 *  - /ws/events/**         → géré par JwtHandshakeInterceptor (pas Spring Security)
 *  - Tout le reste         → authentifié via JWT
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtService jwtService;

    public SecurityConfig(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtService);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Stateless — pas de session HTTP
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // CSRF désactivé (API REST + WebSocket natif)
            .csrf(AbstractHttpConfigurer::disable)

            // Headers pour H2 console (iframe)
            .headers(h -> h.frameOptions(fo -> fo.sameOrigin()))

            .authorizeHttpRequests(auth -> auth
                // Endpoint public : obtention du token
                .requestMatchers("/api/auth/token").permitAll()
                // H2 Console (désactiver en prod)
                .requestMatchers("/h2-console/**").permitAll()
                // WebSocket : la sécurité est gérée par JwtHandshakeInterceptor
                .requestMatchers("/ws/**").permitAll()
                // Tout le reste nécessite une authentification
                .anyRequest().authenticated()
            )

            // Filtre JWT avant l'auth classique
            .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
