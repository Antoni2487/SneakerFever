package com.example.acceso.usuario;

import com.example.acceso.common.ApiResponses;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

/**
 * Login/logout/sesión en JSON para el frontend React, en paralelo al flujo de
 * formulario Thymeleaf existente en LoginController (que sigue funcionando igual
 * mientras dura la migración).
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UsuarioService usuarioService;

    public AuthController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    public record LoginRequest(String usuario, String clave) {
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest request, HttpSession session) {
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

        session.setAttribute("usuarioLogueado", usuarioEncontrado);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Inicio de sesión exitoso",
                "usuario", UsuarioResponse.from(usuarioEncontrado)
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok(Map.of("success", true));
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(HttpSession session) {
        Usuario usuarioLogueado = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuarioLogueado == null) {
            return ApiResponses.error("No hay sesión activa", HttpStatus.UNAUTHORIZED);
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "usuario", UsuarioResponse.from(usuarioLogueado)
        ));
    }
}
