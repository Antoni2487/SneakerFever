package com.example.acceso.usuario;

import com.example.acceso.common.ApiResponses;
import com.example.acceso.perfil.Perfil;
import com.example.acceso.perfil.PerfilResponse;
import com.example.acceso.perfil.PerfilService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/usuarios")
public class UsuarioController {

    private static final Logger log = LoggerFactory.getLogger(UsuarioController.class);
    private static final String PERFIL_ADMINISTRADOR = "Administrador";

    private final UsuarioService usuarioService;
    private final PerfilService perfilService;

    public UsuarioController(UsuarioService usuarioService, PerfilService perfilService) {
        this.usuarioService = usuarioService;
        this.perfilService = perfilService;
    }

    @GetMapping("/listar")
    public String listarUsuarios(Model model) {
        List<Usuario> usuarios = usuarioService.listarUsuarios();
        model.addAttribute("usuarios", usuarios);
        model.addAttribute("formUsuario", new Usuario());
        return "admin/usuarios";
    }

    /**
     * Lista usuarios y envía datos de seguridad al frontend
     */
    @GetMapping("/api/listar")
    @ResponseBody
    public ResponseEntity<?> listarUsuariosApi(HttpSession session) {
        try {
            List<Usuario> usuarios = usuarioService.listarUsuarios();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", usuarios.stream().map(UsuarioResponse::from).toList());

            Usuario usuarioLogueado = (Usuario) session.getAttribute("usuarioLogueado");
            if (usuarioLogueado != null) {
                response.put("currentUserId", usuarioLogueado.getId());
                response.put("currentUserPerfilId", usuarioLogueado.getPerfil() != null
                        ? usuarioLogueado.getPerfil().getId()
                        : null);

                response.put("totalAdmins", contarAdministradoresActivos());
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error al listar usuarios", e);
            return ApiResponses.error("Error al listar usuarios: " + e.getMessage());
        }
    }

    @GetMapping("/api/perfiles")
    @ResponseBody
    public ResponseEntity<?> listarPerfilesActivosApi() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", perfilService.listarPerfilesActivos().stream()
                .map(PerfilResponse::from)
                .toList());
        return ResponseEntity.ok(response);
    }

    /**
     * Guarda o actualiza un usuario con validaciones de seguridad
     */
    @PostMapping("/api/guardar")
    @ResponseBody
    public ResponseEntity<?> guardarUsuarioAjax(
            @Valid @RequestBody UsuarioRequest request,
            BindingResult bindingResult,
            HttpSession session) {

        if (bindingResult.hasErrors()) {
            return ApiResponses.validationError(bindingResult);
        }

        try {
            Usuario usuarioLogueado = (Usuario) session.getAttribute("usuarioLogueado");
            Perfil perfilNuevo = request.getPerfilId() != null
                    ? perfilService.obtenerPerfilPorId(request.getPerfilId()).orElse(null)
                    : null;

            // VALIDACIÓN 1: No puedes cambiar tu propio perfil a uno inferior
            if (request.getId() != null && usuarioLogueado != null
                    && request.getId().equals(usuarioLogueado.getId())) {

                Perfil perfilActual = usuarioLogueado.getPerfil();

                if (perfilActual != null && perfilNuevo != null
                        && PERFIL_ADMINISTRADOR.equalsIgnoreCase(perfilActual.getNombre())
                        && !perfilActual.getId().equals(perfilNuevo.getId())) {

                    return ApiResponses.error(
                            "⛔ ACCIÓN DENEGADA: No puedes cambiar tu propio perfil de Administrador.",
                            HttpStatus.FORBIDDEN);
                }
            }

            // VALIDACIÓN 2: No puedes bajar de Admin al último administrador
            if (request.getId() != null) {
                Usuario usuarioExistente = usuarioService.obtenerUsuarioPorId(request.getId()).orElse(null);
                if (usuarioExistente != null && usuarioExistente.getPerfil() != null) {
                    boolean eraAdmin = PERFIL_ADMINISTRADOR.equalsIgnoreCase(usuarioExistente.getPerfil().getNombre());
                    boolean seraAdmin = perfilNuevo != null
                            && PERFIL_ADMINISTRADOR.equalsIgnoreCase(perfilNuevo.getNombre());

                    if (eraAdmin && !seraAdmin && contarAdministradoresActivos() <= 1) {
                        return ApiResponses.error(
                                "⛔ No puedes cambiar el perfil del único administrador del sistema.",
                                HttpStatus.FORBIDDEN);
                    }
                }
            }

            Usuario usuarioGuardado = usuarioService.guardarUsuario(request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("usuario", UsuarioResponse.from(usuarioGuardado));
            response.put("message", request.getId() != null
                    ? "Usuario actualizado correctamente"
                    : "Usuario creado correctamente");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error al guardar usuario", e);
            return ApiResponses.error("Error interno del servidor: " + e.getMessage());
        }
    }

    @GetMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<?> obtenerUsuario(@PathVariable Long id) {
        try {
            return usuarioService.obtenerUsuarioPorId(id)
                    .map(usuario -> {
                        Map<String, Object> response = new HashMap<>();
                        response.put("success", true);
                        response.put("data", UsuarioResponse.from(usuario));
                        return ResponseEntity.ok(response);
                    })
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error al obtener usuario {}", id, e);
            return ApiResponses.error("Error al obtener usuario: " + e.getMessage());
        }
    }

    /**
     * Elimina un usuario con validaciones de seguridad robustas
     */
    @DeleteMapping("/api/eliminar/{id}")
    @ResponseBody
    public ResponseEntity<?> eliminarUsuarioAjax(@PathVariable Long id, HttpSession session) {
        try {
            Usuario usuarioLogueado = (Usuario) session.getAttribute("usuarioLogueado");
            if (usuarioLogueado != null && usuarioLogueado.getId().equals(id)) {
                return ApiResponses.error(
                        "⛔ ACCIÓN DENEGADA: No puedes eliminar tu propia cuenta.", HttpStatus.FORBIDDEN);
            }

            Usuario usuarioAEliminar = usuarioService.obtenerUsuarioPorId(id).orElse(null);
            if (usuarioAEliminar == null) {
                return ApiResponses.error("Usuario no encontrado", HttpStatus.NOT_FOUND);
            }

            if (usuarioAEliminar.getPerfil() != null
                    && PERFIL_ADMINISTRADOR.equalsIgnoreCase(usuarioAEliminar.getPerfil().getNombre())
                    && usuarioAEliminar.getEstado() == 1
                    && contarAdministradoresActivos() <= 1) {

                return ApiResponses.error(
                        "⛔ ACCIÓN DENEGADA: No puedes eliminar al único administrador activo del sistema.",
                        HttpStatus.FORBIDDEN);
            }

            usuarioService.eliminarUsuario(id);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "✅ Usuario eliminado correctamente"
            ));

        } catch (Exception e) {
            log.error("Error al eliminar usuario {}", id, e);
            return ApiResponses.error("Error al eliminar usuario: " + e.getMessage());
        }
    }

    /**
     * Cambia el estado de un usuario con validaciones de seguridad
     */
    @PostMapping("/api/cambiar-estado/{id}")
    @ResponseBody
    public ResponseEntity<?> cambiarEstadoUsuarioAjax(@PathVariable Long id, HttpSession session) {
        try {
            Usuario usuarioLogueado = (Usuario) session.getAttribute("usuarioLogueado");
            if (usuarioLogueado != null && usuarioLogueado.getId().equals(id)) {
                return ApiResponses.error(
                        "⛔ ACCIÓN DENEGADA: No puedes desactivar tu propia cuenta.", HttpStatus.FORBIDDEN);
            }

            Usuario usuarioACambiar = usuarioService.obtenerUsuarioPorId(id).orElse(null);
            if (usuarioACambiar == null) {
                return ApiResponses.error("Usuario no encontrado", HttpStatus.NOT_FOUND);
            }

            if (usuarioACambiar.getEstado() == 1
                    && usuarioACambiar.getPerfil() != null
                    && PERFIL_ADMINISTRADOR.equalsIgnoreCase(usuarioACambiar.getPerfil().getNombre())
                    && contarAdministradoresActivos() <= 1) {

                return ApiResponses.error(
                        "⛔ ACCIÓN DENEGADA: No puedes desactivar al único administrador activo del sistema.",
                        HttpStatus.FORBIDDEN);
            }

            return usuarioService.cambiarEstadoUsuario(id)
                    .map(usuario -> {
                        Map<String, Object> response = new HashMap<>();
                        response.put("success", true);
                        response.put("usuario", UsuarioResponse.from(usuario));
                        response.put("message", usuario.getEstado() == 1
                                ? "✅ Usuario activado correctamente"
                                : "✅ Usuario desactivado correctamente");
                        return ResponseEntity.ok(response);
                    })
                    .orElseGet(() -> ApiResponses.error("Error al cambiar estado del usuario"));

        } catch (Exception e) {
            log.error("Error al cambiar estado de usuario {}", id, e);
            return ApiResponses.error("Error al cambiar estado: " + e.getMessage());
        }
    }

    /**
     * Método auxiliar para contar administradores activos
     */
    private long contarAdministradoresActivos() {
        return usuarioService.listarUsuarios().stream()
                .filter(u -> u.getEstado() == 1)
                .filter(u -> u.getPerfil() != null && PERFIL_ADMINISTRADOR.equalsIgnoreCase(u.getPerfil().getNombre()))
                .count();
    }
}
