package com.example.acceso.perfil;

import com.example.acceso.perfil.OpcionResponse;
import com.example.acceso.perfil.PerfilRequest;
import com.example.acceso.perfil.PerfilResponse;
import com.example.acceso.perfil.Perfil;
import com.example.acceso.perfil.PerfilService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/perfiles")
public class PerfilController {

    private final PerfilService perfilService;

    public PerfilController(PerfilService perfilService) {
        this.perfilService = perfilService;
    }

    @GetMapping("/listar")
    public String mostrarPaginaPerfiles() {
        return "admin/perfiles"; // Devuelve el nombre de la vista (perfiles.html)
    }

    @GetMapping("/api/listar")
    @ResponseBody
    public ResponseEntity<?> listarPerfilesApi() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", perfilService.listarTodosLosPerfiles().stream()
                .map(PerfilResponse::from)
                .toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<?> obtenerPerfil(@PathVariable Long id) {
        return perfilService.obtenerPerfilPorId(id)
                .map(perfil -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    // Preparamos los datos para el frontend
                    Map<String, Object> perfilData = new HashMap<>();
                    perfilData.put("id", perfil.getId());
                    perfilData.put("nombre", perfil.getNombre());
                    perfilData.put("descripcion", perfil.getDescripcion());
                    perfilData.put("estado", perfil.isEstado());
                    perfilData.put("opciones",
                            perfil.getOpciones().stream().map(op -> op.getId()).collect(Collectors.toSet()));

                    response.put("data", perfilData);
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/api/guardar")
    @ResponseBody
    public ResponseEntity<?> guardarPerfil(@RequestBody PerfilRequest request) {
        Map<String, Object> response = new HashMap<>();
        try {
            Perfil perfilGuardado = perfilService.guardarPerfil(request);
            response.put("success", true);
            response.put("message", request.getId() != null ? "Perfil actualizado" : "Perfil creado");
            response.put("perfil", PerfilResponse.from(perfilGuardado));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al guardar el perfil: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/api/cambiar-estado/{id}")
    @ResponseBody
    public ResponseEntity<?> cambiarEstadoPerfil(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        return perfilService.cambiarEstadoPerfil(id)
                .map(perfil -> {
                    response.put("success", true);
                    response.put("message", "Estado del perfil actualizado");
                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> {
                    response.put("success", false);
                    response.put("message", "Perfil no encontrado");
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
                });
    }

    @GetMapping("/api/opciones")
    @ResponseBody
    public ResponseEntity<?> listarOpcionesApi() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", perfilService.listarTodasLasOpciones().stream()
                .map(OpcionResponse::from)
                .toList());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/api/eliminar/{id}")
    @ResponseBody
    public ResponseEntity<?> eliminarPerfil(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {

            perfilService.eliminarPerfil(id);
            response.put("success", true);
            response.put("message", "Perfil eliminado correctamente");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al eliminar el perfil: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}