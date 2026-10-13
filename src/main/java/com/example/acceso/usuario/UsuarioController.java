package com.example.acceso.usuario;

import org.springframework.security.access.prepost.PreAuthorize;
import com.example.acceso.seguridad.Permisos;
import com.example.acceso.common.ApiResponses;
import com.example.acceso.perfil.Perfil;
import com.example.acceso.perfil.PerfilResponse;
import com.example.acceso.perfil.PerfilService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/usuarios")
@PreAuthorize("hasAuthority('" + Permisos.USUARIOS + "')")
public class UsuarioController {

    private static final Logger log = LoggerFactory.getLogger(UsuarioController.class);

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
    public ResponseEntity<?> listarUsuariosApi(@AuthenticationPrincipal Usuario usuarioLogueado) {
        try {
            List<Usuario> usuarios = usuarioService.listarUsuarios();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", usuarios.stream().map(UsuarioResponse::from).toList());

            if (usuarioLogueado != null) {
                response.put("currentUserId", usuarioLogueado.getId());
                response.put("currentUserPerfilId", usuarioLogueado.getPerfil() != null
                        ? usuarioLogueado.getPerfil().getId()
                        : null);

                response.put("totalAdmins", usuarioService.contarAdministradoresActivos());
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error al listar usuarios", e);
            return ApiResponses.error("Error al listar usuarios: " + ApiResponses.mensajeSeguro(e));
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
            @AuthenticationPrincipal Usuario usuarioLogueado) {

        if (bindingResult.hasErrors()) {
            return ApiResponses.validationError(bindingResult);
        }

        try {
            Perfil perfilNuevo = request.getPerfilId() != null
                    ? perfilService.obtenerPerfilPorId(request.getPerfilId()).orElse(null)
                    : null;

            if (request.getId() != null && usuarioLogueado != null
                    && request.getId().equals(usuarioLogueado.getId())) {
                usuarioService.validarCambioPerfilPropio(usuarioLogueado.getPerfil(), perfilNuevo);
            }

            if (request.getId() != null) {
                Usuario usuarioExistente = usuarioService.obtenerUsuarioPorId(request.getId()).orElse(null);
                if (usuarioExistente != null) {
                    usuarioService.validarDemocionUltimoAdmin(usuarioExistente, perfilNuevo);
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

        } catch (UsuarioService.AccesoDenegadoException e) {
            // Delegado a GlobalExceptionHandler, que lo traduce a 403 -- no
            // se atrapa localmente para no repetir la misma traduccion en
            // cada metodo (ver AUDITORIA_TECNICA, hallazgo #6).
            throw e;
        } catch (Exception e) {
            log.error("Error al guardar usuario", e);
            return ApiResponses.error("Error interno del servidor: " + ApiResponses.mensajeSeguro(e));
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
            return ApiResponses.error("Error al obtener usuario: " + ApiResponses.mensajeSeguro(e));
        }
    }

    /**
     * Elimina un usuario con validaciones de seguridad robustas
     */
    @DeleteMapping("/api/eliminar/{id}")
    @ResponseBody
    public ResponseEntity<?> eliminarUsuarioAjax(@PathVariable Long id, @AuthenticationPrincipal Usuario usuarioLogueado) {
        try {

            Usuario usuarioAEliminar = usuarioService.obtenerUsuarioPorId(id).orElse(null);
            if (usuarioAEliminar == null) {
                return ApiResponses.error("Usuario no encontrado", HttpStatus.NOT_FOUND);
            }

            usuarioService.validarPuedeEliminar(usuarioAEliminar,
                    usuarioLogueado != null ? usuarioLogueado.getId() : null);

            usuarioService.eliminarUsuario(id);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "✅ Usuario eliminado correctamente"
            ));

        } catch (UsuarioService.AccesoDenegadoException e) {
            // Delegado a GlobalExceptionHandler, que lo traduce a 403 -- no
            // se atrapa localmente para no repetir la misma traduccion en
            // cada metodo (ver AUDITORIA_TECNICA, hallazgo #6).
            throw e;
        } catch (Exception e) {
            log.error("Error al eliminar usuario {}", id, e);
            return ApiResponses.error("Error al eliminar usuario: " + ApiResponses.mensajeSeguro(e));
        }
    }

    /**
     * Cambia el estado de un usuario con validaciones de seguridad
     */
    @PostMapping("/api/cambiar-estado/{id}")
    @ResponseBody
    public ResponseEntity<?> cambiarEstadoUsuarioAjax(@PathVariable Long id, @AuthenticationPrincipal Usuario usuarioLogueado) {
        try {

            Usuario usuarioACambiar = usuarioService.obtenerUsuarioPorId(id).orElse(null);
            if (usuarioACambiar == null) {
                return ApiResponses.error("Usuario no encontrado", HttpStatus.NOT_FOUND);
            }

            usuarioService.validarPuedeDesactivar(usuarioACambiar,
                    usuarioLogueado != null ? usuarioLogueado.getId() : null);

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

        } catch (UsuarioService.AccesoDenegadoException e) {
            // Delegado a GlobalExceptionHandler, que lo traduce a 403 -- no
            // se atrapa localmente para no repetir la misma traduccion en
            // cada metodo (ver AUDITORIA_TECNICA, hallazgo #6).
            throw e;
        } catch (Exception e) {
            log.error("Error al cambiar estado de usuario {}", id, e);
            return ApiResponses.error("Error al cambiar estado: " + ApiResponses.mensajeSeguro(e));
        }
    }
}
