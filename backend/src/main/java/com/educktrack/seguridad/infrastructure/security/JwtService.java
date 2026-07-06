package com.educktrack.seguridad.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;

/**
 * Genera y valida tokens JWT firmados con HMAC-SHA (RS-04). Emite sesiones sin
 * estado con expiracion configurable (RNF-06).
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMinutes;
    private final String issuer;

    public JwtService(JwtProperties props) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(props.secret()));
        this.expirationMinutes = props.expirationMinutes();
        this.issuer = props.issuer();
    }

    /**
     * Emite un token para el usuario autenticado (RF-60). El subject es el correo
     * institucional; los roles viajan como claim para el RBAC (RS-03).
     */
    public String generarToken(String correo, List<String> roles) {
        Instant ahora = Instant.now();
        Instant expira = ahora.plusSeconds(expirationMinutes * 60);
        return Jwts.builder()
                .subject(correo)
                .issuer(issuer)
                .claim("roles", roles)
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(expira))
                .signWith(key)
                .compact();
    }

    /** Extrae y valida las claims del token; lanza JwtException si es invalido/expirado. */
    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extraerCorreo(String token) {
        return parseClaims(token).getSubject();
    }

    @SuppressWarnings("unchecked")
    public List<String> extraerRoles(String token) {
        Object roles = parseClaims(token).get("roles");
        return roles instanceof List ? (List<String>) roles : List.of();
    }

    /** Segundos de expiracion, util para informar al cliente (RF-60). */
    public long getExpiracionSegundos() {
        return expirationMinutes * 60;
    }
}
