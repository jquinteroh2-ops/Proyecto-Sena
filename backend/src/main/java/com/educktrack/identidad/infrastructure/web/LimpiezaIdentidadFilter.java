package com.educktrack.identidad.infrastructure.web;

import com.educktrack.identidad.application.IdentidadDeLaPeticion;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Vacia la memoria de identidad al terminar cada peticion.
 *
 * <p>Va el <strong>primero</strong> de la cadena y limpia en un {@code finally},
 * de modo que ninguna salida —respuesta normal, excepcion o error del propio
 * filtro de seguridad— pueda dejar identidad colgando en el hilo. El servidor
 * reutiliza los hilos entre peticiones: una identidad que sobrevive al final de
 * la peticion se la encuentra la siguiente que use ese hilo, y eso es un fallo
 * de aislamiento, no una fuga de memoria.</p>
 *
 * <p>Limpia al salir y no al entrar a proposito: limpiar al entrar dejaria la
 * ultima identidad viva en el hilo durante todo el tiempo que ese hilo pase
 * inactivo en el pool.</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LimpiezaIdentidadFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } finally {
            IdentidadDeLaPeticion.limpiar();
        }
    }
}
