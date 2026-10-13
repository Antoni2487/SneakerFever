package com.example.acceso.seguridad;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Autenticación JWT stateless (ver {@link JwtService} / {@link JwtAuthenticationFilter}).
 * Reemplaza a SessionInterceptor + HttpSession, retirados en la migración a JWT.
 *
 * <ul>
 *   <li><b>Denegar por defecto</b>: lo público es una lista blanca explícita y
 *       cada grupo de endpoints de dominio exige la autoridad derivada de su
 *       {@code Opcion} (ver {@link JwtAuthenticationFilter}); lo demás es
 *       {@code denyAll()}.</li>
 *   <li><b>RBAC por Opción</b>: {@code /admin/api/**} (dashboard) exige la
 *       autoridad {@link JwtAuthenticationFilter#AUTORIDAD_DASHBOARD};
 *       {@code /api/upload/{tipo}/**} la del dominio dueño de esa carpeta.</li>
 *   <li><b>CSRF sigue deshabilitado</b>: los endpoints de negocio van con
 *       {@code Authorization: Bearer}, sin cookie ambiental; la superficie CSRF
 *       real se reduce a {@code /api/auth/refresh} y {@code /api/auth/logout}
 *       (cookie httpOnly {@code SameSite=Strict} del refresh token).</li>
 * </ul>
 *
 * El panel admin Thymeleaf legacy ({@code LoginController} + plantillas
 * {@code admin/*.html}) queda inaccesible: su login solo escribe HttpSession y
 * nunca produce un JWT. Se retira en el Bloque E del plan.
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
                        // Va ANTES del permitAll de "/carrito/api/**" (gana la primera
                        // regla que coincide): ver/agregar al carrito es público, pero
                        // finalizar la compra exige JWT — CartController.checkout toma
                        // el Usuario del SecurityContext.
                        .requestMatchers(HttpMethod.POST, "/carrito/api/checkout").authenticated()
                        // Lista blanca EXPLÍCITA de lo público: sitio web, carrito,
                        // login y recursos estáticos. Todo lo que no figure aquí ni en
                        // las reglas de abajo se deniega (anyRequest().denyAll()).
                        .requestMatchers(
                                // Sitio público Thymeleaf (WebController) y checkout legacy,
                                // que valida su propia sesión (CheckoutController).
                                "/", "/catalogo", "/contacto", "/producto/**",
                                "/carrito/api/**", "/carrito/checkout", "/carrito/procesar",
                                "/api/auth/**", "/login", "/logout",
                                "/api/public/**", "/personalizacion/api/public/**",
                                "/web/**", "/css/**", "/js/**", "/images/**", "/uploads/**",
                                "/error", "/favicon.ico")
                        .permitAll()
                        .requestMatchers("/admin/api/**").hasAuthority(JwtAuthenticationFilter.AUTORIDAD_DASHBOARD)
                        // Cada carpeta de imágenes la gestiona su dominio: antes bastaba
                        // estar logueado para subir o BORRAR imágenes de cualquiera.
                        .requestMatchers("/api/upload/productos/**").hasAuthority("productos")
                        .requestMatchers("/api/upload/marcas/**").hasAuthority("marcas")
                        .requestMatchers("/api/upload/categorias/**").hasAuthority("categorias")
                        .requestMatchers("/api/upload/personalizacion/**").hasAuthority("personalizacion")
                        .requestMatchers("/marcas/api/**").hasAuthority("marcas")
                        .requestMatchers("/categorias/api/**").hasAuthority("categorias")
                        .requestMatchers("/clientes/api/**").hasAuthority("clientes")
                        .requestMatchers("/productos/api/**").hasAuthority("productos")
                        .requestMatchers("/usuarios/api/**").hasAuthority("usuarios")
                        .requestMatchers("/perfiles/api/**").hasAuthority("perfiles")
                        // Créditos vive como pestaña dentro de la misma pantalla de
                        // Ventas (una sola Opcion "Ventas" en BD) — misma autoridad.
                        .requestMatchers("/ventas/api/**", "/creditos/api/**").hasAuthority("ventas")
                        .requestMatchers("/inventario/api/**").hasAuthority("inventario")
                        .requestMatchers("/personalizacion/api/**").hasAuthority("personalizacion")
                        // Denegar por defecto: un endpoint nuevo sin regla explícita da
                        // 403 en vez de quedar público sin que nadie lo note. También
                        // cubre las vistas Thymeleaf legacy del panel (/admin,
                        // /productos, /ventas/nueva...), inservibles desde la migración
                        // a JWT porque su login nunca produce un token.
                        .anyRequest().denyAll())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(
                                (request, response, authException) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED))
                        .accessDeniedHandler(
                                (request, response, accessDeniedException) -> response.sendError(HttpServletResponse.SC_FORBIDDEN)))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
