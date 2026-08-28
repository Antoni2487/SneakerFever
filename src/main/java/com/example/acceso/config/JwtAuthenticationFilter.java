package com.example.acceso.config;

import com.example.acceso.usuario.Usuario;
import com.example.acceso.usuario.UsuarioService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

/**
 * Lee "Authorization: Bearer &lt;token&gt;", valida el access token y, si es
 * válido, carga el {@link Usuario} real desde BD (no confía solo en los
 * claims) para poder rechazar en caliente a un usuario desactivado después de
 * emitirse el token — propiedad que la sesión anterior no tenía (el Usuario
 * quedaba cacheado en HttpSession hasta el logout o la expiración de sesión).
 *
 * Sin autorización por rol todavía (RBAC queda para una fase separada del
 * plan de AUDITORIA_INTEGRAL.md): cualquier token válido de un usuario activo
 * autentica, sin distinguir permisos.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UsuarioService usuarioService;

    public JwtAuthenticationFilter(JwtService jwtService, UsuarioService usuarioService) {
        this.jwtService = jwtService;
        this.usuarioService = usuarioService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Claims claims = jwtService.parseAndValidate(token, JwtService.TOKEN_TYPE_ACCESS);
                String username = claims.getSubject();

                Optional<Usuario> usuarioOpt = usuarioService.findByUsuario(username);
                if (usuarioOpt.isPresent() && usuarioOpt.get().getEstado() == 1) {
                    Usuario usuario = usuarioOpt.get();
                    Authentication authentication = new UsernamePasswordAuthenticationToken(
                            usuario, null, Collections.emptyList());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
                // Si el usuario no existe o está inactivo, se deja sin
                // autenticar a propósito — el token es válido pero la cuenta
                // ya no lo es.
            } catch (JwtException | IllegalArgumentException e) {
                // Token ausente de forma válida, expirado, manipulado o del
                // tipo equivocado: se deja sin autenticar. El propio
                // SecurityFilterChain responde 401 más abajo si la ruta lo
                // requiere; no se registra como error porque es un caso
                // esperado (token expirado en cada renovación normal).
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
