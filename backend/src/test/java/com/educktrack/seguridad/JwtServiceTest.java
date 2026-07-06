package com.educktrack.seguridad;

import com.educktrack.seguridad.infrastructure.security.JwtProperties;
import com.educktrack.seguridad.infrastructure.security.JwtService;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas del servicio JWT (RS-04): emision, lectura y rechazo de tokens.
 */
class JwtServiceTest {

    // Secreto de prueba (Base64, >= 256 bits).
    private static final String SECRET =
            "ZGV2LXNlY3JldC1lZHVja3RyYWNrLWNhbWJpYXItZW4tcHJvZHVjY2lvbi0xMjM0NTY3OA==";

    private final JwtService jwt = new JwtService(new JwtProperties(SECRET, 480, "educktrack"));

    @Test
    void emiteYLeeElCorreoYRolesDelToken() {
        String token = jwt.generarToken("ana@colegio.edu.co", List.of("DOCENTE", "COORDINADOR_ACADEMICO"));

        assertEquals("ana@colegio.edu.co", jwt.extraerCorreo(token));
        assertTrue(jwt.extraerRoles(token).contains("DOCENTE"));
        assertTrue(jwt.extraerRoles(token).contains("COORDINADOR_ACADEMICO"));
    }

    @Test
    void rechazaUnTokenManipulado() {
        String token = jwt.generarToken("ana@colegio.edu.co", List.of("DOCENTE"));
        String manipulado = token.substring(0, token.length() - 2) + "xx";

        assertThrows(JwtException.class, () -> jwt.extraerCorreo(manipulado));
    }

    @Test
    void rechazaUnTokenEmitidoConOtroSecreto() {
        JwtService otro = new JwtService(new JwtProperties(
                "b3Ryby1zZWNyZXRvLWRpc3RpbnRvLXBhcmEtcHJ1ZWJhcy1lZHVja3RyYWNrLTk4NzY1NA==", 480, "educktrack"));
        String tokenAjeno = otro.generarToken("x@x.edu.co", List.of("ESTUDIANTE"));

        assertThrows(JwtException.class, () -> jwt.extraerCorreo(tokenAjeno));
    }
}
