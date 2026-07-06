package com.educktrack.seguridad.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.educktrack.shared.web.ApiError;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.time.OffsetDateTime;

/**
 * Configuracion central de seguridad (RS-03 RBAC, RS-04 JWT, RS-05 BCrypt).
 *
 * <p>Sustituye la configuracion permisiva de la Fase 0. La API es sin estado:
 * cada peticion se autentica por JWT mediante {@link JwtAuthenticationFilter}.
 * El control de acceso fino por rol se aplica con {@code @PreAuthorize} en los
 * controladores de cada modulo (habilitado por {@link EnableMethodSecurity}).</p>
 */
@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    private static final String[] RUTAS_PUBLICAS = {
            "/api/auth/login",
            "/api/auth/recuperar-password",
            "/api/health",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, ObjectMapper objectMapper) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.objectMapper = objectMapper;
    }

    /** BCrypt con 10 rondas (RS-05, RNF-05). */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(RUTAS_PUBLICAS).permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex.authenticationEntryPoint(this::responder401))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /** Responde 401 en JSON coherente con {@link ApiError} (RNF-10). */
    private void responder401(jakarta.servlet.http.HttpServletRequest request,
                              HttpServletResponse response,
                              org.springframework.security.core.AuthenticationException ex)
            throws java.io.IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiError body = new ApiError(OffsetDateTime.now(), 401, "Unauthorized", null,
                "Debe autenticarse para acceder a este recurso.", null, request.getRequestURI());
        objectMapper.writeValue(response.getWriter(), body);
    }
}
