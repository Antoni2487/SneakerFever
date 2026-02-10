package com.example.acceso.controller;

import com.example.acceso.model.Usuario;
import com.example.acceso.model.Perfil;
import com.example.acceso.service.PerfilService;
import com.example.acceso.service.UsuarioService;
import org.springframework.stereotype.Controller;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/usuarios")
public class UsuarioController {

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
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<Usuario> usuarios = usuarioService.listarUsuarios();
            response.put("success", true);
            response.put("data", usuarios);
            
            // ⚡ SEGURIDAD: Enviar información del usuario logueado
            // ⚡ CORREGIDO: Usar "usuarioLogueado" en lugar de "usuario"
            Usuario usuarioLogueado = (Usuario) session.getAttribute("usuarioLogueado");
            if (usuarioLogueado != null) {
                response.put("currentUserId", usuarioLogueado.getId());
                response.put("currentUserPerfilId", usuarioLogueado.getPerfil() != null 
                    ? usuarioLogueado.getPerfil().getId() 
                    : null);
                
                // ⚡ Contar admins activos (para validación de "último admin")
                long totalAdmins = usuarios.stream()
                    .filter(u -> u.getEstado() == 1) // Solo activos
                    .filter(u -> u.getPerfil() != null && "Administrador".equalsIgnoreCase(u.getPerfil().getNombre()))
                    .count();
                
                response.put("totalAdmins", totalAdmins);
                
                System.out.println("👤 Usuario logueado: ID=" + usuarioLogueado.getId() + 
                                   ", Perfil=" + (usuarioLogueado.getPerfil() != null ? usuarioLogueado.getPerfil().getNombre() : "null"));
                System.out.println("🔑 Perfil ID:" + (usuarioLogueado.getPerfil() != null ? usuarioLogueado.getPerfil().getId() : "null"));
                System.out.println("👥 Total Admins activos: " + totalAdmins);
            } else {
                System.out.println("⚠️ NO HAY USUARIO EN SESIÓN");
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Error al listar usuarios: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/api/perfiles")
    @ResponseBody
    public ResponseEntity<?> listarPerfilesActivosApi() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", perfilService.listarPerfilesActivos());
        return ResponseEntity.ok(response);
    }

    /**
     * Guarda o actualiza un usuario con validaciones de seguridad
     */
    @PostMapping("/api/guardar")
    @ResponseBody
    public ResponseEntity<?> guardarUsuarioAjax(
            @Valid @RequestBody Usuario usuario, 
            BindingResult bindingResult,
            HttpSession session) {
        
        Map<String, Object> response = new HashMap<>();

        if (bindingResult.hasErrors()) {
            Map<String, String> errores = new HashMap<>();
            bindingResult.getFieldErrors().forEach(error -> 
                errores.put(error.getField(), error.getDefaultMessage())
            );
            response.put("success", false);
            response.put("message", "Datos inválidos");
            response.put("errors", errores);
            return ResponseEntity.badRequest().body(response);
        }

        try {
            // ⚡ CORREGIDO: Usar "usuarioLogueado"
            Usuario usuarioLogueado = (Usuario) session.getAttribute("usuarioLogueado");
            
            // ⚡ VALIDACIÓN 1: No puedes cambiar tu propio perfil a uno inferior
            if (usuario.getId() != null && usuarioLogueado != null 
                && usuario.getId().equals(usuarioLogueado.getId())) {
                
                Perfil perfilActual = usuarioLogueado.getPerfil();
                Perfil perfilNuevo = usuario.getPerfil();
                
                if (perfilActual != null && perfilNuevo != null) {
                    // Si eres admin y intentas cambiar a otro perfil
                    if ("Administrador".equalsIgnoreCase(perfilActual.getNombre()) 
                        && !perfilActual.getId().equals(perfilNuevo.getId())) {
                        
                        response.put("success", false);
                        response.put("message", "⛔ ACCIÓN DENEGADA: No puedes cambiar tu propio perfil de Administrador.");
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
                    }
                }
            }
            
            // ⚡ VALIDACIÓN 2: Si estás cambiando el perfil de alguien de Admin a otro
            // y es el último admin, no permitirlo
            if (usuario.getId() != null) {
                Usuario usuarioExistente = usuarioService.obtenerUsuarioPorId(usuario.getId()).orElse(null);
                if (usuarioExistente != null && usuarioExistente.getPerfil() != null) {
                    boolean eraAdmin = "Administrador".equalsIgnoreCase(usuarioExistente.getPerfil().getNombre());
                    boolean seraAdmin = usuario.getPerfil() != null 
                        && "Administrador".equalsIgnoreCase(usuario.getPerfil().getNombre());
                    
                    if (eraAdmin && !seraAdmin) {
                        long totalAdmins = contarAdministradoresActivos();
                        if (totalAdmins <= 1) {
                            response.put("success", false);
                            response.put("message", "⛔ No puedes cambiar el perfil del único administrador del sistema.");
                            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
                        }
                    }
                }
            }

            Usuario usuarioGuardado = usuarioService.guardarUsuario(usuario);
            response.put("success", true);
            response.put("usuario", usuarioGuardado);
            response.put("message", usuario.getId() != null 
                ? "Usuario actualizado correctamente" 
                : "Usuario creado correctamente");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Error interno del servidor: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<?> obtenerUsuario(@PathVariable Long id) {
        try {
            return usuarioService.obtenerUsuarioPorId(id).map(usuario -> {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("data", usuario);
                return ResponseEntity.ok(response);
            }).orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error al obtener usuario: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Elimina un usuario con validaciones de seguridad robustas
     */
    @DeleteMapping("/api/eliminar/{id}")
    @ResponseBody
    public ResponseEntity<?> eliminarUsuarioAjax(@PathVariable Long id, HttpSession session) {
        Map<String, Object> response = new HashMap<>();

        try {
            // 🛡️ VALIDACIÓN 1: No puedes eliminarte a ti mismo
            // ⚡ CORREGIDO: Usar "usuarioLogueado"
            Usuario usuarioLogueado = (Usuario) session.getAttribute("usuarioLogueado");
            if (usuarioLogueado != null && usuarioLogueado.getId().equals(id)) {
                response.put("success", false);
                response.put("message", "⛔ ACCIÓN DENEGADA: No puedes eliminar tu propia cuenta.");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

            // 🛡️ VALIDACIÓN 2: Verificar si el usuario existe
            Usuario usuarioAEliminar = usuarioService.obtenerUsuarioPorId(id).orElse(null);
            if (usuarioAEliminar == null) {
                response.put("success", false);
                response.put("message", "Usuario no encontrado");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            // 🛡️ VALIDACIÓN 3: No puedes eliminar al último administrador activo
            if (usuarioAEliminar.getPerfil() != null 
                && "Administrador".equalsIgnoreCase(usuarioAEliminar.getPerfil().getNombre())
                && usuarioAEliminar.getEstado() == 1) {
                
                long totalAdmins = contarAdministradoresActivos();
                if (totalAdmins <= 1) {
                    response.put("success", false);
                    response.put("message", "⛔ ACCIÓN DENEGADA: No puedes eliminar al único administrador activo del sistema.");
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
                }
            }

            // Todo OK, proceder con la eliminación
            usuarioService.eliminarUsuario(id);
            response.put("success", true);
            response.put("message", "✅ Usuario eliminado correctamente");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Error al eliminar usuario: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Cambia el estado de un usuario con validaciones de seguridad
     */
    @PostMapping("/api/cambiar-estado/{id}")
    @ResponseBody
    public ResponseEntity<?> cambiarEstadoUsuarioAjax(@PathVariable Long id, HttpSession session) {
        Map<String, Object> response = new HashMap<>();

        try {
            // 🛡️ VALIDACIÓN 1: No puedes desactivarte a ti mismo
            // ⚡ CORREGIDO: Usar "usuarioLogueado"
            Usuario usuarioLogueado = (Usuario) session.getAttribute("usuarioLogueado");
            if (usuarioLogueado != null && usuarioLogueado.getId().equals(id)) {
                response.put("success", false);
                response.put("message", "⛔ ACCIÓN DENEGADA: No puedes desactivar tu propia cuenta.");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

            // 🛡️ VALIDACIÓN 2: Verificar si existe el usuario
            Usuario usuarioACambiar = usuarioService.obtenerUsuarioPorId(id).orElse(null);
            if (usuarioACambiar == null) {
                response.put("success", false);
                response.put("message", "Usuario no encontrado");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            // 🛡️ VALIDACIÓN 3: No puedes desactivar al último admin activo
            if (usuarioACambiar.getEstado() == 1 
                && usuarioACambiar.getPerfil() != null
                && "Administrador".equalsIgnoreCase(usuarioACambiar.getPerfil().getNombre())) {
                
                long totalAdmins = contarAdministradoresActivos();
                if (totalAdmins <= 1) {
                    response.put("success", false);
                    response.put("message", "⛔ ACCIÓN DENEGADA: No puedes desactivar al único administrador activo del sistema.");
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
                }
            }

            // Todo OK, proceder con el cambio de estado
            return usuarioService.cambiarEstadoUsuario(id)
                    .map(usuario -> {
                        response.put("success", true);
                        response.put("usuario", usuario);
                        response.put("message", usuario.getEstado() == 1 
                            ? "✅ Usuario activado correctamente" 
                            : "✅ Usuario desactivado correctamente");
                        return ResponseEntity.ok(response);
                    })
                    .orElseGet(() -> {
                        response.put("success", false);
                        response.put("message", "Error al cambiar estado del usuario");
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
                    });
                    
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Error al cambiar estado: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Método auxiliar para contar administradores activos
     */
    private long contarAdministradoresActivos() {
        List<Usuario> usuarios = usuarioService.listarUsuarios();
        return usuarios.stream()
                .filter(u -> u.getEstado() == 1) // Solo activos
                .filter(u -> u.getPerfil() != null && "Administrador".equalsIgnoreCase(u.getPerfil().getNombre()))
                .count();
    }
}