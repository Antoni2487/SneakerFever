package com.example.acceso.controller;

import com.example.acceso.model.Brand;
import com.example.acceso.service.BrandService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
@RequestMapping("/marcas")
public class BrandController {

    private static final Logger log = LoggerFactory.getLogger(BrandController.class);

    private final BrandService brandService;

    public BrandController(BrandService brandService) {
        this.brandService = brandService;
    }

    // ===================== Vistas =====================

    @GetMapping({"", "/"})
    public String listarMarcas(Model model) {
        model.addAttribute("title", "Gestión de Marcas");
        model.addAttribute("totalMarcas", brandService.contarMarcas());
        model.addAttribute("totalTodasMarcas", brandService.contarTodasMarcas());
        return "admin/marcas";
    }

    @GetMapping("/listar")
    public String listarMarcasRedirect() {
        return "redirect:/marcas";
    }

    // ===================== API REST - Listados =====================

    @GetMapping("/api/listar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> listarMarcasJson() {
        try {
            List<Brand> marcas = brandService.listarMarcas();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", marcas,
                "total", marcas.size()
            ));
        } catch (Exception e) {
            log.error("Error al listar marcas", e);
            return createInternalErrorResponse("Error al listar marcas");
        }
    }

    @GetMapping("/api/datatables")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> listarParaDataTables() {
        try {
            List<Brand> marcas = brandService.listarTodasMarcas();
            return ResponseEntity.ok(Map.of(
                "draw", 1,
                "recordsTotal", marcas.size(),
                "recordsFiltered", marcas.size(),
                "data", marcas
            ));
        } catch (Exception e) {
            log.error("Error al cargar datos para DataTables", e);
            return createInternalErrorResponse("Error al cargar datos para la tabla");
        }
    }

    // ===================== API REST - CRUD =====================

    @GetMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> obtenerMarca(@PathVariable Long id) {
        try {
            Optional<Brand> marca = brandService.obtenerMarcaPorId(id);

            if (marca.isPresent()) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", marca.get()
                ));
            }

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "success", false,
                "message", "Marca no encontrada"
            ));
        } catch (Exception e) {
            log.error("Error al obtener marca con ID: {}", id, e);
            return createInternalErrorResponse("Error al obtener la marca");
        }
    }

    @PostMapping("/api/crear")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> crearMarca(
            @RequestParam("nombre") String nombre,
            @RequestParam(value = "imagenUrl", required = false) String imagenUrl
    ) {
        try {
            // Validar nombre
            ValidationResult validacion = validarNombre(nombre);
            if (!validacion.isValid()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", validacion.getMessage()
                ));
            }

            // Verificar duplicados
            if (brandService.existeMarca(nombre.trim())) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "success", false,
                    "message", "Ya existe una marca con ese nombre"
                ));
            }

            // Crear marca
            Brand nuevaMarca = new Brand();
            nuevaMarca.setNombre(nombre.trim());
            nuevaMarca.setEstado(1);

            // Asignar URL de imagen si se proporcionó
            if (imagenUrl != null && !imagenUrl.trim().isEmpty()) {
                nuevaMarca.setImagen(imagenUrl.trim());
            }

            Brand guardada = brandService.guardarMarca(nuevaMarca);

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "success", true,
                "message", "Marca creada exitosamente",
                "data", guardada
            ));

        } catch (BrandService.MarcaException e) {
            log.warn("Error de negocio al crear marca: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error inesperado al crear marca", e);
            return createInternalErrorResponse("Error al crear la marca");
        }
    }

    @PutMapping("/api/actualizar/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> actualizarMarca(
            @PathVariable Long id,
            @RequestBody Map<String, String> datos  // ✅ CAMBIO AQUÍ
    ) {
        try {
            String nombre = datos.get("nombre");  // ✅ OBTENER DE MAP
            String imagenUrl = datos.get("imagenUrl");  // ✅ OBTENER DE MAP

            // Validar nombre
            ValidationResult validacion = validarNombre(nombre);
            if (!validacion.isValid()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", validacion.getMessage()
                ));
            }

            // Verificar existencia
            Optional<Brand> optionalBrand = brandService.obtenerMarcaPorId(id);
            if (!optionalBrand.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "success", false,
                    "message", "Marca no encontrada"
                ));
            }

            // Verificar duplicados
            if (brandService.existeMarcaParaActualizar(nombre.trim(), id)) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "success", false,
                    "message", "Ya existe otra marca con ese nombre"
                ));
            }

            // Actualizar marca
            Brand marca = optionalBrand.get();
            marca.setNombre(nombre.trim());

            // Actualizar URL de imagen si se proporcionó
            if (imagenUrl != null && !imagenUrl.trim().isEmpty()) {
                marca.setImagen(imagenUrl.trim());
            }

            Brand actualizada = brandService.guardarMarca(marca);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Marca actualizada exitosamente",
                "data", actualizada
            ));

        } catch (BrandService.MarcaException e) {
            log.warn("Error de negocio al actualizar marca {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error inesperado al actualizar marca {}", id, e);
            return createInternalErrorResponse("Error al actualizar la marca");
        }
    }

    @DeleteMapping("/api/eliminar/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> eliminarMarca(@PathVariable Long id) {
        try {
            brandService.eliminarMarca(id);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Marca eliminada exitosamente"
            ));
        } catch (BrandService.MarcaException e) {
            log.warn("Error de negocio al eliminar marca {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error inesperado al eliminar marca {}", id, e);
            return createInternalErrorResponse("Error al eliminar la marca");
        }
    }

    @PutMapping("/api/cambiar-estado/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> cambiarEstado(@PathVariable Long id) {
        try {
            Optional<Brand> marca = brandService.cambiarEstadoMarca(id);

            if (marca.isPresent()) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Estado cambiado exitosamente",
                    "data", marca.get()
                ));
            }

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "success", false,
                "message", "Marca no encontrada"
            ));
        } catch (Exception e) {
            log.error("Error al cambiar estado de marca {}", id, e);
            return createInternalErrorResponse("Error al cambiar el estado");
        }
    }

    // ===================== API REST - Gestión de Imágenes =====================

    /**
     * Actualizar solo la imagen de una marca
     * Se usa después de subir la imagen con FileUploadController
     */
    @PutMapping("/api/{id}/imagen")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> actualizarImagenMarca(
            @PathVariable Long id,
            @RequestParam("imagenUrl") String imagenUrl) {
        try {
            if (imagenUrl == null || imagenUrl.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "La URL de la imagen es requerida"
                ));
            }

            Brand actualizada = brandService.actualizarImagen(id, imagenUrl.trim());

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Imagen actualizada exitosamente",
                "data", actualizada
            ));

        } catch (BrandService.MarcaException e) {
            log.warn("Error al actualizar imagen de marca {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error inesperado al actualizar imagen de marca {}", id, e);
            return createInternalErrorResponse("Error al actualizar la imagen");
        }
    }

    /**
     * Eliminar la imagen de una marca
     */
    @DeleteMapping("/api/{id}/imagen")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> eliminarImagenMarca(@PathVariable Long id) {
        try {
            Brand actualizada = brandService.eliminarImagen(id);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Imagen eliminada exitosamente",
                "data", actualizada
            ));

        } catch (BrandService.MarcaException e) {
            log.warn("Error al eliminar imagen de marca {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error inesperado al eliminar imagen de marca {}", id, e);
            return createInternalErrorResponse("Error al eliminar la imagen");
        }
    }

    // ===================== API REST - Búsquedas =====================

    @GetMapping("/api/buscar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> buscarMarcas(@RequestParam String q) {
        try {
            if (q == null || q.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Parámetro de búsqueda requerido"
                ));
            }

            List<Brand> marcas = brandService.buscarMarcas(q.trim());
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", marcas,
                "total", marcas.size()
            ));
        } catch (Exception e) {
            log.error("Error en búsqueda con query: {}", q, e);
            return createInternalErrorResponse("Error en la búsqueda");
        }
    }

    @GetMapping("/api/activas")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> marcasActivas() {
        try {
            List<Brand> marcas = brandService.listarMarcas();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", marcas
            ));
        } catch (Exception e) {
            log.error("Error al obtener marcas activas", e);
            return createInternalErrorResponse("Error al obtener marcas activas");
        }
    }

    @GetMapping("/api/con-imagen")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> marcasConImagen() {
        try {
            List<Brand> marcas = brandService.listarMarcasConImagen();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", marcas,
                "total", marcas.size()
            ));
        } catch (Exception e) {
            log.error("Error al obtener marcas con imagen", e);
            return createInternalErrorResponse("Error al obtener marcas con imagen");
        }
    }

    @GetMapping("/api/sin-imagen")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> marcasSinImagen() {
        try {
            List<Brand> marcas = brandService.listarMarcasSinImagen();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", marcas,
                "total", marcas.size()
            ));
        } catch (Exception e) {
            log.error("Error al obtener marcas sin imagen", e);
            return createInternalErrorResponse("Error al obtener marcas sin imagen");
        }
    }

    // ===================== Métodos de Validación =====================

    private ValidationResult validarNombre(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            return ValidationResult.invalid("El nombre es requerido");
        }

        String nombreTrim = nombre.trim();

        if (nombreTrim.length() < 2) {
            return ValidationResult.invalid("El nombre debe tener al menos 2 caracteres");
        }

        if (nombreTrim.length() > 100) {
            return ValidationResult.invalid("El nombre no puede exceder 100 caracteres");
        }

        return ValidationResult.valid();
    }

    // ===================== Métodos de Utilidad =====================

    private ResponseEntity<Map<String, Object>> createInternalErrorResponse(String message) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
            "success", false,
            "message", message
        ));
    }

    // Clase interna para resultados de validación
    private static class ValidationResult {
        private final boolean valid;
        private final String message;

        private ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public static ValidationResult valid() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult invalid(String message) {
            return new ValidationResult(false, message);
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }
    }
}