package com.educktrack.seguridad.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades de configuracion del JWT (RS-04). Se enlazan desde
 * {@code educktrack.security.jwt.*} en application.yml.
 *
 * @param secret            clave secreta en Base64 (>= 256 bits)
 * @param expirationMinutes minutos de validez del token (RNF-06: 8h = 480)
 * @param issuer            emisor del token
 */
@ConfigurationProperties(prefix = "educktrack.security.jwt")
public record JwtProperties(String secret, long expirationMinutes, String issuer) {
}
