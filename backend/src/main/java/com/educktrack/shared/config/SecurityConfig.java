package com.educktrack.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuracion de seguridad base.
 *
 * <p>DECISION DE DISENO: en la Fase 0 esta configuracion es permisiva para
 * permitir arrancar el scaffold y exponer Swagger UI. La Fase 2 (RS-03 RBAC,
 * RS-04 JWT) la reemplaza por un filtro JWT + autorizacion por rol. El bean
 * {@link PasswordEncoder} ya usa BCrypt con 10 rondas (RS-05, RNF-05) y se
 * mantiene en las fases siguientes.</p>
 */
@Configuration
public class SecurityConfig {

    /** BCrypt con 10 rondas de trabajo (RS-05, RNF-05). */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // DECISION DE DISENO (Fase 0): todo abierto temporalmente.
                // Fase 2 restringira por rol (RS-03) y exigira JWT (RS-04).
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
