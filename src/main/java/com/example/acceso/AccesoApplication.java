// Define el paquete al que pertenece la clase.
package com.example.acceso;

// Importaciones de clases necesarias de Spring Boot.
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

// Se excluye UserDetailsServiceAutoConfiguration: la autenticación es JWT manual
// (JwtService + JwtAuthenticationFilter), no se usa AuthenticationManager ni
// UserDetailsService de Spring Security, así que sin este exclude Boot genera
// igual un usuario en memoria con contraseña aleatoria en cada arranque (que
// nunca se usa) y lo loguea como WARN.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class AccesoApplication {


	public static void main(String[] args) {
		SpringApplication.run(AccesoApplication.class, args);
	}
}
