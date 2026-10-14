package com.example.acceso.seguridad;

import com.example.acceso.common.ApiResponses;
import com.example.acceso.usuario.Usuario;
import com.example.acceso.usuario.UsuarioResponse;
import com.example.acceso.usuario.UsuarioService;
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

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String REFRESH_COOKIE_NAME = "refreshToken";
    private static final String REFRESH_COOKIE_PATH = "/api/auth";

    private final UsuarioService usuarioService;
    private final JwtService jwtService;
    private final RevocacionTokenService revocacionTokenService;

    @Value("${jwt.cookie.secure:false}")
    private boolean cookieSecure;

    public AuthController(UsuarioService usuarioService, JwtService jwtService,
            RevocacionTokenService revocacionTokenService) {
        this.usuarioService = usuarioService;
        this.jwtService = jwtService;
        this.revocacionTokenService = revocacionTokenService;
    }

    public record LoginRequest(String usuario, String clave) {
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest request,
            HttpServletResponse response) {
        Optional<Usuario> usuarioOpt = usuarioService.findByUsuarioConPerfil(request.usuario());

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
        if (revocacionTokenService.estaRevocado(claims.getId())) {
            return ApiResponses.error("La sesión fue cerrada", HttpStatus.UNAUTHORIZED);
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

    /**
     * Cierra la sesión en el servidor: revoca el refresh token (cookie) y el
     * access token (header Authorization) de ESTA sesión, así dejan de servir
     * aunque alguien los haya copiado. Las sesiones de otros dispositivos
     * siguen abiertas. Responde 200 aunque falte alguno o ya esté vencido:
     * cerrar sesión debe funcionar siempre.
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(
            @CookieValue(value = REFRESH_COOKIE_NAME, required = false) String refreshToken,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            HttpServletResponse response) {
        revocarSiEsValido(refreshToken, JwtService.TOKEN_TYPE_REFRESH);
        if (authorization != null && authorization.startsWith("Bearer ")) {
            revocarSiEsValido(authorization.substring(7), JwtService.TOKEN_TYPE_ACCESS);
        }
        response.addHeader(HttpHeaders.SET_COOKIE, buildExpiredRefreshCookie().toString());
        return ResponseEntity.ok(Map.of("success", true));
    }

    private void revocarSiEsValido(String token, String tipo) {
        if (token == null || token.isBlank()) {
            return;
        }
        try {
            revocacionTokenService.revocar(jwtService.parseAndValidate(token, tipo));
        } catch (JwtException | IllegalArgumentException e) {
            // Vencido, manipulado o de otro tipo: no hay nada que revocar.
        }
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
