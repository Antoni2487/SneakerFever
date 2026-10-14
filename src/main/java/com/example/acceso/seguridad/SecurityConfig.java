package com.example.acceso.seguridad;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Autenticación JWT stateless (ver {@link JwtService} / {@link JwtAuthenticationFilter}).
 * Reemplaza a SessionInterceptor + HttpSession, retirados en la migración a JWT.
 *
 * <ul>
 *   <li><b>Denegar por defecto</b>: lo público es una lista blanca explícita,
 *       las APIs del panel exigen autenticación y lo demás es
 *       {@code denyAll()}.</li>
 *   <li><b>Permisos por método</b> ({@link EnableMethodSecurity}): cada
 *       controller declara su permiso con
 *       {@code @PreAuthorize("hasAuthority('" + Permisos.PRODUCTOS + "')")}; las
 *       autoridades del usuario salen de {@code opciones.permiso} (ver
 *       {@link JwtAuthenticationFilter}). {@code ControllersPreAuthorizeTest}
 *       falla si un endpoint del panel queda sin {@code @PreAuthorize}.</li>
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
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * CORS: por defecto ningún origen externo (el frontend va por el mismo origen:
     * proxy de Vite en desarrollo, nginx en Docker). Si el frontend se sirve desde
     * otro dominio, se habilita con {@code app.cors.allowed-origins} (lista
     * separada por comas). {@code allowCredentials} es necesario para la cookie
     * httpOnly del refresh token.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:}") List<String> allowedOrigins) {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        List<String> origenes = allowedOrigins.stream().map(String::trim).filter(o -> !o.isEmpty()).toList();
        if (!origenes.isEmpty()) {
            CorsConfiguration config = new CorsConfiguration();
            config.setAllowedOrigins(origenes);
            config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
            config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
            config.setAllowCredentials(true);
            source.registerCorsConfiguration("/**", config);
        }
        return source;
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
                        // APIs del panel: aquí solo se exige estar autenticado. El permiso
                        // fino (PRODUCTOS, VENTAS, ...) lo declara cada controller con
                        // @PreAuthorize, junto al código que protege — ver Permisos.
                        .requestMatchers(
                                "/admin/api/**", "/api/upload/**",
                                "/marcas/api/**", "/categorias/api/**", "/clientes/api/**",
                                "/productos/api/**", "/usuarios/api/**", "/perfiles/api/**",
                                "/ventas/api/**", "/creditos/api/**", "/inventario/api/**",
                                "/personalizacion/api/**")
                        .authenticated()
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
