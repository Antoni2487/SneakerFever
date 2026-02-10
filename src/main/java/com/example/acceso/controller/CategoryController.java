package com.example.acceso.controller;

import com.example.acceso.model.Category;
import com.example.acceso.service.CategoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/categorias")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService)
    {
        this.categoryService = categoryService;
    }

    // ===================== Vistas =====================

    /**
     * Página principal de gestión de categorías
     */
    @GetMapping({"", "/"})
       public String listarCategorias(Model model) {
           // menuOpciones ya se configura en LoginController
           model.addAttribute("title", "Gestión de Categorías");
           model.addAttribute("totalCategorias", categoryService.contarCategorias());
           model.addAttribute("totalTodasCategorias", categoryService.contarTodasCategorias());
           return "admin/categorias";
       }

       @GetMapping("/listar")
       public String listarCategoriasRedirect() {
           return "redirect:/categorias";
       }


    // ===================== API REST - Listados =====================

    /**
     * Obtener todas las categorías activas (JSON)
     */
    @GetMapping("/api/listar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> listarCategoriasJson() {
        try {
            List<Category> categorias = categoryService.listarCategorias();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", categorias);
            response.put("total", categorias.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse("Error al listar categorías: " + e.getMessage());
        }
    }

    /**
     * Obtener todas las categorías (activas e inactivas) para DataTables
     */
    @GetMapping("/api/datatables")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> listarParaDataTables() {
        try {
            List<Category> categorias = categoryService.listarTodasCategorias();

            Map<String, Object> response = new HashMap<>();
            response.put("draw", 1);
            response.put("recordsTotal", categorias.size());
            response.put("recordsFiltered", categorias.size());
            response.put("data", categorias);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse("Error al cargar datos para la tabla: " + e.getMessage());
        }
    }

    // ===================== API REST - CRUD =====================

    /**
     * Obtener categoría por ID
     */
    @GetMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> obtenerCategoria(@PathVariable Long id) {
        try {
            Optional<Category> categoria = categoryService.obtenerCategoriaPorId(id);

            Map<String, Object> response = new HashMap<>();
            if (categoria.isPresent()) {
                response.put("success", true);
                response.put("data", categoria.get());
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Categoría no encontrada");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (Exception e) {
            return createErrorResponse("Error al obtener la categoría: " + e.getMessage());
        }
    }

    /**
     * Crear nueva categoría
     */
    @PostMapping("/api/crear")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> crearCategoria(@Valid @RequestBody Category categoria, BindingResult result) {
        if (result.hasErrors()) {
            return createValidationErrorResponse(result);
        }

        try {
            // Validar duplicados
            if (categoryService.existeCategoria(categoria.getNombre())) {
                return createErrorResponse("Ya existe una categoría con ese nombre");
            }

            Category nuevaCategoria = categoryService.guardarCategoria(categoria);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Categoría creada exitosamente");
            response.put("data", nuevaCategoria);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (CategoryService.CategoriaException e) {
            return createErrorResponse(e.getMessage());
        } catch (Exception e) {
            return createErrorResponse("Error al crear la categoría: " + e.getMessage());
        }
    }

    /**
     * Actualizar categoría existente
     */
    @PutMapping("/api/actualizar/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> actualizarCategoria(
            @PathVariable Long id,
            @Valid @RequestBody Category categoria,
            BindingResult result) {

        if (result.hasErrors()) {
            return createValidationErrorResponse(result);
        }

        try {
            // Verificar que la categoría existe
            if (!categoryService.obtenerCategoriaPorId(id).isPresent()) {
                return createErrorResponse("Categoría no encontrada");
            }

            // Validar duplicados (excluyendo la actual)
            if (categoryService.existeCategoriaParaActualizar(categoria.getNombre(), id)) {
                return createErrorResponse("Ya existe otra categoría con ese nombre");
            }

            categoria.setId(id);
            Category categoriaActualizada = categoryService.guardarCategoria(categoria);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Categoría actualizada exitosamente");
            response.put("data", categoriaActualizada);

            return ResponseEntity.ok(response);

        } catch (CategoryService.CategoriaException e) {
            return createErrorResponse(e.getMessage());
        } catch (Exception e) {
            return createErrorResponse("Error al actualizar la categoría: " + e.getMessage());
        }
    }

    /**
     * Eliminar categoría (eliminación lógica)
     */
    @DeleteMapping("/api/eliminar/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> eliminarCategoria(@PathVariable Long id) {
        try {
            categoryService.eliminarCategoria(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Categoría eliminada exitosamente");

            return ResponseEntity.ok(response);

        } catch (CategoryService.CategoriaException e) {
            return createErrorResponse(e.getMessage());
        } catch (Exception e) {
            return createErrorResponse("Error al eliminar la categoría: " + e.getMessage());
        }
    }

    /**
     * Cambiar estado de categoría (activar/desactivar)
     */
    @PutMapping("/api/cambiar-estado/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> cambiarEstado(@PathVariable Long id) {
        try {
            Optional<Category> categoria = categoryService.cambiarEstadoCategoria(id);

            Map<String, Object> response = new HashMap<>();
            if (categoria.isPresent()) {
                response.put("success", true);
                response.put("message", "Estado cambiado exitosamente");
                response.put("data", categoria.get());
                return ResponseEntity.ok(response);
            } else {
                return createErrorResponse("Categoría no encontrada");
            }

        } catch (Exception e) {
            return createErrorResponse("Error al cambiar el estado: " + e.getMessage());
        }
    }

    // ===================== API REST - Búsquedas =====================

    /**
     * Buscar categorías por nombre
     */
    @GetMapping("/api/buscar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> buscarCategorias(@RequestParam String q) {
        try {
            List<Category> categorias = categoryService.buscarCategorias(q);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", categorias);
            response.put("total", categorias.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse("Error en la búsqueda: " + e.getMessage());
        }
    }

    /**
     * Obtener solo categorías activas (para selects)
     */
    @GetMapping("/api/activas")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> categoriasActivas() {
        try {
            List<Category> categorias = categoryService.listarCategorias();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", categorias);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse("Error al obtener categorías activas: " + e.getMessage());
        }
    }

    // ===================== Métodos de utilidad =====================

    /**
     * Crear respuesta de error estándar
     */
    private ResponseEntity<Map<String, Object>> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * Crear respuesta de errores de validación
     */
    private ResponseEntity<Map<String, Object>> createValidationErrorResponse(BindingResult result) {
        Map<String, Object> response = new HashMap<>();
        Map<String, String> errors = new HashMap<>();

        result.getFieldErrors().forEach(error ->
            errors.put(error.getField(), error.getDefaultMessage())
        );

        response.put("success", false);
        response.put("message", "Errores de validación");
        response.put("errors", errors);

        return ResponseEntity.badRequest().body(response);
    }
}