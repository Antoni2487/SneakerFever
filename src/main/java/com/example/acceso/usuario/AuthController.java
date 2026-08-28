package com.example.acceso.usuario;

import com.example.acceso.common.ApiResponses;
import com.example.acceso.config.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Login/logout/sesión en JSON para el frontend React, ahora vía JWT (access +
 * refresh token) en vez de HttpSession — ver AUDITORIA_INTEGRAL.md, sección 9
 * (Bloque A).
 *
 * <p>El refresh token viaja SOLO en una cookie httpOnly con {@code path=/api/auth}
 * (nunca en el body JSON, para que no quede accesible desde JavaScript); el
 * access token viaja en el body de /login y /refresh, y el cliente lo manda en
 * cada petición como header {@code Authorization: Bearer <token>}.
 *
 * <p><b>Limitación conocida (documentada, no un descuido):</b> no hay
 * whitelist/blacklist de refresh tokens en el servidor. /logout borra la
 * cookie del navegador que hizo logout, pero un refresh token que ya se haya
 * copiado/filtrado fuera de esa cookie sigue siendo válido hasta su
 * expiración natural. Implementar revocación real requeriría una tabla/cache
 * de tokens emitidos — se deja fuera de esta fase a propósito (ver plan de la
 * auditoría, RBAC/CSRF/rate-limiting van en fases separadas y esto es del
 * mismo tamaño de esfuerzo).
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String REFRESH_COOKIE_NAME = "refreshToken";
    private static final String REFRESH_COOKIE_PATH = "/api/auth";

    private final UsuarioService usuarioService;
    private final JwtService jwtService;

    @Value("${jwt.cookie.secure:false}")
    private boolean cookieSecure;

    public AuthController(UsuarioService usuarioService, JwtService jwtService) {
        this.usuarioService = usuarioService;
        this.jwtService = jwtService;
    }

    public record LoginRequest(String usuario, String clave) {
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest request,
            HttpServletResponse response) {
        Optional<Usuario> usuarioOpt = usuarioService.findByUsuario(request.usuario());

        if (usuarioOpt.isEmpty()) {
            return ApiResponses.error("Usuario no encontrado", HttpStatus.UNAUTHORIZED);
        }

        Usuario usuarioEncontrado = usuarioOpt.get();

        if (usuarioEncontrado.getEstado() != 1) {
            return ApiResponses.error("Este usuario se encuentra inactivo", HttpStatus.FORBIDDEN);
        }

        if (!usuarioService.verificarContrasena(request.clave(), usuarioEncontrado.getClave())) {
            return ApiResponses.error("Contraseña incorrecta", HttpStatus.UNAUTHORIZED);
        }

        String accessToken = jwtService.generateAccessToken(usuarioEncontrado);
        String refreshToken = jwtService.generateRefreshToken(usuarioEncontrado);
        response.addHeader(HttpHeaders.SET_COOKIE, buildRefreshCookie(refreshToken).toString());

        Map<String, Object> body = new HashMap<>();
        body.put("success", true);
        body.put("message", "Inicio de sesión exitoso");
        body.put("accessToken", accessToken);
        body.put("usuario", UsuarioResponse.from(usuarioEncontrado));
        return ResponseEntity.ok(body);
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refresh(
            @CookieValue(value = REFRESH_COOKIE_NAME, required = false) String refreshToken) {

        if (refreshToken == null) {
            return ApiResponses.error("No hay refresh token", HttpStatus.UNAUTHORIZED);
        }

        Claims claims;
        try {
            claims = jwtService.parseAndValidate(refreshToken, JwtService.TOKEN_TYPE_REFRESH);
        } catch (JwtException | IllegalArgumentException e) {
            return ApiResponses.error("Refresh token inválido o expirado", HttpStatus.UNAUTHORIZED);
        }

        Optional<Usuario> usuarioOpt = usuarioService.findByUsuario(claims.getSubject());
        if (usuarioOpt.isEmpty() || usuarioOpt.get().getEstado() != 1) {
            return ApiResponses.error("Usuario no encontrado o inactivo", HttpStatus.UNAUTHORIZED);
        }

        String newAccessToken = jwtService.generateAccessToken(usuarioOpt.get());

        Map<String, Object> body = new HashMap<>();
        body.put("success", true);
        body.put("accessToken", newAccessToken);
        return ResponseEntity.ok(body);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpServletResponse response) {
        // Sin blacklist de servidor (ver limitación documentada en la clase):
        // esto solo borra la cookie de ESTE navegador.
        response.addHeader(HttpHeaders.SET_COOKIE, buildExpiredRefreshCookie().toString());
        return ResponseEntity.ok(Map.of("success", true));
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Usuario usuarioAutenticado)) {
            return ApiResponses.error("No hay sesión activa", HttpStatus.UNAUTHORIZED);
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "usuario", UsuarioResponse.from(usuarioAutenticado)));
    }

    private ResponseCookie buildRefreshCookie(String token) {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, token)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Strict")
                .path(REFRESH_COOKIE_PATH)
                .maxAge(jwtService.getRefreshTokenExpirationSeconds())
                .build();
    }

    private ResponseCookie buildExpiredRefreshCookie() {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Strict")
                .path(REFRESH_COOKIE_PATH)
                .maxAge(0)
                .build();
    }
}
