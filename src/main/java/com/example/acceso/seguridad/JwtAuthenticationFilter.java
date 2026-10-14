package com.example.acceso.seguridad;

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
 * RBAC: las {@link GrantedAuthority} del usuario son los
 * {@link Opcion#getPermiso() códigos de permiso} de las opciones de su
 * {@link Perfil} (ver {@link Permisos}). Cada controller exige el suyo con
 * {@code @PreAuthorize("hasAuthority(...)")}. La ruta de la opción ya no
 * interviene: es solo el link del sidebar.
 *
 * Un access token revocado con logout ({@link RevocacionTokenService}) se trata
 * como ausente aunque su firma y expiración sean válidas.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UsuarioService usuarioService;
    private final RevocacionTokenService revocacionTokenService;

    public JwtAuthenticationFilter(JwtService jwtService, UsuarioService usuarioService,
            RevocacionTokenService revocacionTokenService) {
        this.jwtService = jwtService;
        this.usuarioService = usuarioService;
        this.revocacionTokenService = revocacionTokenService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Claims claims = jwtService.parseAndValidate(token, JwtService.TOKEN_TYPE_ACCESS);
                if (revocacionTokenService.estaRevocado(claims.getId())) {
                    // Token cerrado con logout: firma válida, pero ya no sirve.
                    filterChain.doFilter(request, response);
                    return;
                }
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
                .map(Opcion::getPermiso)
                .filter(permiso -> permiso != null && !permiso.isBlank())
                .distinct()
                .<GrantedAuthority>map(SimpleGrantedAuthority::new)
                .toList();
    }
}
