package com.example.acceso.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

// @Configuration: Anotación que le indica a Spring que esta clase contiene
// definiciones de beans. Los beans son objetos gestionados por Spring que
// pueden ser inyectados en otras partes de la aplicación.
@Configuration
public class SecurityConfig {

    // @Bean: Esta anotación se aplica a un método para indicar que devuelve un
    // objeto
    // que debe ser registrado como un bean en el contexto de Spring.
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        // BCryptPasswordEncoder es una implementación para encriptar contraseñas
        // utilizando el algoritmo BCrypt. Al crearlo como un bean, podemos inyectarlo
        // y usarlo en cualquier parte de la aplicación (como en UsuarioService).
        return new BCryptPasswordEncoder();
    }
}