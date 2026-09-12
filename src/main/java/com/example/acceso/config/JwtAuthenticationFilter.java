package com.example.acceso.config;

import com.example.acceso.perfil.Opcion;
import com.example.acceso.perfil.Perfil;
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
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Lee "Authorization: Bearer &lt;token&gt;", valida el access token y, si es
 * válido, carga el {@link Usuario} real desde BD (no confía solo en los
 * claims) para poder rechazar en caliente a un usuario desactivado después de
 * emitirse el token — propiedad que la sesión anterior no tenía (el Usuario
 * quedaba cacheado en HttpSession hasta el logout o la expiración de sesión).
 *
 * RBAC (Fase A del plan de AUDITORIA_TECNICA_2026-09-10.md): las
 * {@link GrantedAuthority} del usuario se derivan del primer segmento de
 * {@link Opcion#getRuta()} de cada opción asignada a su {@link Perfil} (p.
 * ej. "/productos" o "/usuarios/listar" → autoridad "productos"/"usuarios").
 * Ese segmento coincide por construcción con la ruta base de cada dominio
 * (el link del sidebar en React usa exactamente ese mismo "ruta", así que ya
 * tiene que coincidir con la ruta real del frontend para funcionar hoy). Ver
 * {@link SecurityConfig} para el {@code hasAuthority(...)} por endpoint.
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

                // Con el Perfil y sus Opciones ya inicializados: este filtro corre
                // antes de que open-in-view abra el EntityManager de la petición,
                // así que el Usuario llega detached al controlador y serializar su
                // perfil con fetch lazy reventaba en /api/auth/me.
                Optional<Usuario> usuarioOpt = usuarioService.findByUsuarioConPerfil(username);
                if (usuarioOpt.isPresent() && usuarioOpt.get().getEstado() == 1) {
                    Usuario usuario = usuarioOpt.get();
                    Authentication authentication = new UsernamePasswordAuthenticationToken(
                            usuario, null, authoridadesDe(usuario));
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

    private List<GrantedAuthority> authoridadesDe(Usuario usuario) {
        Perfil perfil = usuario.getPerfil();
        if (perfil == null) {
            return List.of();
        }
        return perfil.getOpciones().stream()
                .map(Opcion::getRuta)
                .map(this::primerSegmento)
                .filter(segmento -> !segmento.isEmpty())
                .distinct()
                .<GrantedAuthority>map(SimpleGrantedAuthority::new)
                .toList();
    }

    /** "/productos" -&gt; "productos", "/usuarios/listar" -&gt; "usuarios", "/" o null/"" -&gt; "". */
    private String primerSegmento(String ruta) {
        if (ruta == null) {
            return "";
        }
        for (String parte : ruta.split("/")) {
            if (!parte.isBlank()) {
                return parte.toLowerCase();
            }
        }
        return "";
    }
}
