package com.example.acceso.usuario;

import com.example.acceso.perfil.Opcion;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Comparator;
import java.util.Optional;

// TODO(auth-jwt-migration): flujo de login por formulario Thymeleaf, DEPRECADO.
// Desde la migración a JWT (AUDITORIA_INTEGRAL.md sección 9, Bloque A),
// SecurityConfig protege /admin/** y las rutas /*/api/** exigiendo un JWT por
// header Authorization — un login hecho aquí (que solo escribe
// HttpSession.setAttribute("usuarioLogueado", ...)) YA NO da acceso a esas
// rutas. Se deja sin borrar solo porque CheckoutController todavía lee este
// mismo atributo de sesión para el checkout público (fuera del alcance de
// esta migración). Se retira junto con el resto de Thymeleaf legacy en el
// Bloque E del plan de la auditoría.
@Controller
public class LoginController {
    // Inyección de dependencia del servicio de usuario.
    private final UsuarioService usuarioService;

    public LoginController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    // Maneja las peticiones GET a /login. Muestra el formulario de inicio de
    // sesión.
    @GetMapping("/login")
    public String mostrarFormularioLogin(HttpSession session) {
        // Comprueba si ya existe un usuario en la sesión actual.
        if (session.getAttribute("usuarioLogueado") != null) {
            return "redirect:/admin";
        }
        // Si no ha iniciado sesión, muestra la página de login.
        return "admin/login";
    }

    // Maneja las peticiones POST a /login, que se envían desde el formulario.
   @PostMapping("/login")
    public String procesarLogin(@RequestParam String usuario, @RequestParam String clave, HttpSession session,
            RedirectAttributes redirectAttributes) {
        
        Optional<Usuario> usuarioOpt = usuarioService.findByUsuario(usuario);

        if (usuarioOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Usuario no encontrado.");
            return "redirect:/login";
        }

        Usuario usuarioEncontrado = usuarioOpt.get();

        if (usuarioEncontrado.getEstado() != 1) { 
            redirectAttributes.addFlashAttribute("error", "Este usuario se encuentra inactivo.");
            return "redirect:/login";
        }

        if (usuarioService.verificarContrasena(clave, usuarioEncontrado.getClave())) {
            // 1. Guardamos al usuario en sesión
            session.setAttribute("usuarioLogueado", usuarioEncontrado);

            // 2. Cargamos sus opciones de menú (Tu lógica original)
            var opcionesMenu = usuarioEncontrado.getPerfil().getOpciones().stream()
                    .sorted(Comparator.comparing(Opcion::getId))
                    .toList();
            session.setAttribute("menuOpciones", opcionesMenu);

            // ====================================================================
            // 🚀 NUEVA LÓGICA: DETECCIÓN DEL CARRITO
            // ====================================================================
            
            // Recuperamos el carrito de la sesión
            com.example.acceso.carrito.Carrito carrito = (com.example.acceso.carrito.Carrito) session.getAttribute("carrito");

            // Si el carrito existe y TIENE productos...
            if (carrito != null && !carrito.getItems().isEmpty()) {
              
                return "redirect:/carrito/checkout";
            }
            
            return "redirect:/admin"; 
            
        } else {
            redirectAttributes.addFlashAttribute("error", "Contraseña incorrecta.");
            return "redirect:/login";
        }
    }

    // Maneja las peticiones GET a /logout.
    @GetMapping("/logout")
    public String logout(HttpSession session, RedirectAttributes redirectAttributes) {
        // Invalida la sesión, eliminando todos los atributos guardados (incluyendo
        // "usuarioLogueado").
        session.invalidate();
        // Añade un mensaje de éxito para mostrar en la página de login.
        redirectAttributes.addFlashAttribute("logout", "Has cerrado sesión exitosamente.");
        return "redirect:/login";
    }
}