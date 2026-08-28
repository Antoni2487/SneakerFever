package com.example.acceso.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Autenticación JWT stateless (ver {@link JwtService} / {@link JwtAuthenticationFilter}).
 * Reemplaza a SessionInterceptor + HttpSession, retirados en esta misma
 * migración — ver AUDITORIA_INTEGRAL.md, sección 9 (Bloque A).
 *
 * Alcance deliberado de ESTA fase (no toca lo que sigue — son fases
 * separadas del plan de la auditoría):
 * <ul>
 *   <li><b>Sin autorización por rol/permiso todavía</b>: cualquier usuario
 *       autenticado puede llamar cualquier endpoint protegido, exactamente
 *       igual que con SessionInterceptor. El hueco de RBAC de la auditoría
 *       (hallazgo #1) sigue abierto — se cierra en la fase siguiente.</li>
 *   <li><b>CSRF sigue deshabilitado</b>: los endpoints de negocio ya no
 *       dependen de una cookie de sesión ambiental (van con
 *       {@code Authorization: Bearer}), lo que reduce la superficie CSRF real
 *       a solo {@code /api/auth/refresh} y {@code /api/auth/logout} (los
 *       únicos que leen la cookie httpOnly del refresh token) — se revisa a
 *       fondo en la fase de CSRF del plan (hallazgo #2).</li>
 *   <li>La lista de rutas protegidas replica EXACTAMENTE la que tenía
 *       {@code SessionInterceptor} (ver commit anterior de WebConfig), para
 *       no ampliar ni reducir la superficie de seguridad como efecto
 *       colateral de esta migración.</li>
 * </ul>
 *
 * Efecto colateral conocido y aceptado: el panel admin Thymeleaf legacy
 * ({@code LoginController} + plantillas {@code admin/*.html}) deja de poder
 * acceder a las rutas protegidas de abajo, porque su login solo escribe un
 * atributo en HttpSession y nunca produce un JWT — Spring Security no lo
 * reconoce como autenticado. No queda abierto: queda inaccesible (401). Es el
 * mismo flujo que ya está marcado para retirarse en el Bloque E del plan.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter)
            throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/**",
                                "/login",
                                "/logout",
                                "/personalizacion/api/public/**",
                                "/api/public/**",
                                "/carrito/api/**",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/uploads/**",
                                "/error",
                                "/favicon.ico")
                        .permitAll()
                        .requestMatchers(
                                "/admin/**",
                                "/marcas/api/**",
                                "/categorias/api/**",
                                "/clientes/api/**",
                                "/productos/api/**",
                                "/usuarios/api/**",
                                "/perfiles/api/**",
                                "/ventas/api/**",
                                "/creditos/api/**",
                                "/inventario/api/**",
                                "/personalizacion/api/**",
                                "/api/upload/**")
                        .authenticated()
                        // Todo lo demás (sitio público servido por WebController,
                        // CheckoutController, etc.) queda igual que antes: nunca
                        // estuvo cubierto por SessionInterceptor.
                        .anyRequest().permitAll())
                .exceptionHandling(ex -> ex.authenticationEntryPoint(
                        (request, response, authException) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED)))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
