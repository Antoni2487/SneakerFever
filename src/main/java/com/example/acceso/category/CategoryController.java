package com.example.acceso.category;

import com.example.acceso.common.ApiResponses;
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
            List<CategoryResponse> categorias = categoryService.listarCategorias().stream()
                    .map(CategoryResponse::from)
                    .toList();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", categorias);
            response.put("total", categorias.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ApiResponses.error("Error al listar categorías: " + e.getMessage());
        }
    }

    /**
     * Obtener todas las categorías (activas e inactivas) para DataTables
     */
    @GetMapping("/api/datatables")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> listarParaDataTables() {
        try {
            List<CategoryResponse> categorias = categoryService.listarTodasCategorias().stream()
                    .map(CategoryResponse::from)
                    .toList();

            Map<String, Object> response = new HashMap<>();
            response.put("draw", 1);
            response.put("recordsTotal", categorias.size());
            response.put("recordsFiltered", categorias.size());
            response.put("data", categorias);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ApiResponses.error("Error al cargar datos para la tabla: " + e.getMessage());
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
                response.put("data", CategoryResponse.from(categoria.get()));
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Categoría no encontrada");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (Exception e) {
            return ApiResponses.error("Error al obtener la categoría: " + e.getMessage());
        }
    }

    /**
     * Crear nueva categoría
     */
    @PostMapping("/api/crear")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> crearCategoria(@Valid @RequestBody CategoryRequest request, BindingResult result) {
        if (result.hasErrors()) {
            return ApiResponses.validationError(result);
        }

        try {
            // Validar duplicados
            if (categoryService.existeCategoria(request.getNombre())) {
                return ApiResponses.error("Ya existe una categoría con ese nombre");
            }

            Category nuevaCategoria = categoryService.guardarCategoria(request.toEntity());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Categoría creada exitosamente");
            response.put("data", CategoryResponse.from(nuevaCategoria));

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (CategoryService.CategoriaException e) {
            return ApiResponses.error(e.getMessage());
        } catch (Exception e) {
            return ApiResponses.error("Error al crear la categoría: " + e.getMessage());
        }
    }

    /**
     * Actualizar categoría existente
     */
    @PutMapping("/api/actualizar/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> actualizarCategoria(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request,
            BindingResult result) {

        if (result.hasErrors()) {
            return ApiResponses.validationError(result);
        }

        try {
            // Verificar que la categoría existe
            if (!categoryService.obtenerCategoriaPorId(id).isPresent()) {
                return ApiResponses.error("Categoría no encontrada");
            }

            // Validar duplicados (excluyendo la actual)
            if (categoryService.existeCategoriaParaActualizar(request.getNombre(), id)) {
                return ApiResponses.error("Ya existe otra categoría con ese nombre");
            }

            Category categoria = request.toEntity();
            categoria.setId(id);
            Category categoriaActualizada = categoryService.guardarCategoria(categoria);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Categoría actualizada exitosamente");
            response.put("data", CategoryResponse.from(categoriaActualizada));

            return ResponseEntity.ok(response);

        } catch (CategoryService.CategoriaException e) {
            return ApiResponses.error(e.getMessage());
        } catch (Exception e) {
            return ApiResponses.error("Error al actualizar la categoría: " + e.getMessage());
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
            return ApiResponses.error(e.getMessage());
        } catch (Exception e) {
            return ApiResponses.error("Error al eliminar la categoría: " + e.getMessage());
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
                response.put("data", CategoryResponse.from(categoria.get()));
                return ResponseEntity.ok(response);
            } else {
                return ApiResponses.error("Categoría no encontrada");
            }

        } catch (Exception e) {
            return ApiResponses.error("Error al cambiar el estado: " + e.getMessage());
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
            List<CategoryResponse> categorias = categoryService.buscarCategorias(q).stream()
                    .map(CategoryResponse::from)
                    .toList();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", categorias);
            response.put("total", categorias.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ApiResponses.error("Error en la búsqueda: " + e.getMessage());
        }
    }

    /**
     * Obtener solo categorías activas (para selects)
     */
    @GetMapping("/api/activas")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> categoriasActivas() {
        try {
            List<CategoryResponse> categorias = categoryService.listarCategorias().stream()
                    .map(CategoryResponse::from)
                    .toList();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", categorias);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ApiResponses.error("Error al obtener categorías activas: " + e.getMessage());
        }
    }

}