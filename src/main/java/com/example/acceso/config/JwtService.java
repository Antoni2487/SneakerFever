package com.example.acceso.config;

import com.example.acceso.usuario.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * Generación y validación de JWT (access + refresh) para el login de React.
 * Reemplaza la autenticación por HttpSession — ver AUDITORIA_INTEGRAL.md,
 * sección 9 (Bloque A).
 */
@Service
public class JwtService {

    public static final String TOKEN_TYPE_ACCESS = "access";
    public static final String TOKEN_TYPE_REFRESH = "refresh";

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token.expiration-minutes:30}")
    private long accessTokenExpirationMinutes;

    @Value("${jwt.refresh-token.expiration-days:7}")
    private long refreshTokenExpirationDays;

    private SecretKey signingKey() {
        // HS256 exige una clave de al menos 256 bits (32 bytes) — jjwt lanza
        // WeakKeyException si jwt.secret es más corto que eso.
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(Usuario usuario) {
        return buildToken(usuario, TOKEN_TYPE_ACCESS, Duration.ofMinutes(accessTokenExpirationMinutes));
    }

    public String generateRefreshToken(Usuario usuario) {
        return buildToken(usuario, TOKEN_TYPE_REFRESH, Duration.ofDays(refreshTokenExpirationDays));
    }

    /** Usado para calcular el Max-Age de la cookie httpOnly del refresh token. */
    public long getRefreshTokenExpirationSeconds() {
        return Duration.ofDays(refreshTokenExpirationDays).toSeconds();
    }

    private String buildToken(Usuario usuario, String type, Duration ttl) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(usuario.getUsuario())
                .claim("userId", usuario.getId())
                .claim("type", type)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(signingKey())
                .compact();
    }

    /**
     * Valida firma y expiración, y que el token sea del tipo esperado
     * ("access" o "refresh") — evita que un refresh token se use como access
     * token en un endpoint de negocio, o viceversa en /api/auth/refresh.
     *
     * @throws JwtException si el token es inválido, expiró, o es del tipo
     *                       equivocado (incluye subclases como
     *                       ExpiredJwtException/SignatureException/etc.)
     */
    public Claims parseAndValidate(String token, String expectedType) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        String actualType = claims.get("type", String.class);
        if (!expectedType.equals(actualType)) {
            throw new JwtException("Tipo de token inválido: se esperaba '" + expectedType + "'");
        }
        return claims;
    }
}
