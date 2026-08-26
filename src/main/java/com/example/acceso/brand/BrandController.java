package com.example.acceso.brand;

import com.example.acceso.common.ApiResponses;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
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
            List<BrandResponse> marcas = brandService.listarMarcas().stream()
                    .map(BrandResponse::from)
                    .toList();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", marcas,
                    "total", marcas.size()
            ));
        } catch (Exception e) {
            log.error("Error al listar marcas", e);
            return ApiResponses.error("Error al listar marcas");
        }
    }

    @GetMapping("/api/datatables")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> listarParaDataTables() {
        try {
            List<BrandResponse> marcas = brandService.listarTodasMarcas().stream()
                    .map(BrandResponse::from)
                    .toList();
            return ResponseEntity.ok(Map.of(
                    "draw", 1,
                    "recordsTotal", marcas.size(),
                    "recordsFiltered", marcas.size(),
                    "data", marcas
            ));
        } catch (Exception e) {
            log.error("Error al cargar datos para DataTables", e);
            return ApiResponses.error("Error al cargar datos para la tabla");
        }
    }

    // ===================== API REST - CRUD =====================

    @GetMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> obtenerMarca(@PathVariable Long id) {
        try {
            return brandService.obtenerMarcaPorId(id)
                    .map(marca -> ResponseEntity.ok(Map.of(
                            "success", true,
                            "data", BrandResponse.from(marca)
                    )))
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                            "success", false,
                            "message", "Marca no encontrada"
                    )));
        } catch (Exception e) {
            log.error("Error al obtener marca con ID: {}", id, e);
            return ApiResponses.error("Error al obtener la marca");
        }
    }

    @PostMapping("/api/crear")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> crearMarca(
            @Valid @RequestBody BrandRequest request, BindingResult result) {
        if (result.hasErrors()) {
            return ApiResponses.validationError(result);
        }

        try {
            if (brandService.existeMarca(request.getNombre().trim())) {
                return ApiResponses.error("Ya existe una marca con ese nombre", HttpStatus.CONFLICT);
            }

            Brand guardada = brandService.guardarMarca(request.toEntity());

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "success", true,
                    "message", "Marca creada exitosamente",
                    "data", BrandResponse.from(guardada)
            ));

        } catch (BrandService.MarcaException e) {
            log.warn("Error de negocio al crear marca: {}", e.getMessage());
            return ApiResponses.error(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("Error inesperado al crear marca", e);
            return ApiResponses.error("Error al crear la marca");
        }
    }

    @PutMapping("/api/actualizar/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> actualizarMarca(
            @PathVariable Long id, @Valid @RequestBody BrandRequest request, BindingResult result) {
        if (result.hasErrors()) {
            return ApiResponses.validationError(result);
        }

        try {
            if (brandService.obtenerMarcaPorId(id).isEmpty()) {
                return ApiResponses.error("Marca no encontrada", HttpStatus.NOT_FOUND);
            }

            if (brandService.existeMarcaParaActualizar(request.getNombre().trim(), id)) {
                return ApiResponses.error("Ya existe otra marca con ese nombre", HttpStatus.CONFLICT);
            }

            Brand marca = request.toEntity();
            marca.setId(id);
            Brand actualizada = brandService.guardarMarca(marca);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Marca actualizada exitosamente",
                    "data", BrandResponse.from(actualizada)
            ));

        } catch (BrandService.MarcaException e) {
            log.warn("Error de negocio al actualizar marca {}: {}", id, e.getMessage());
            return ApiResponses.error(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("Error inesperado al actualizar marca {}", id, e);
            return ApiResponses.error("Error al actualizar la marca");
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
            return ApiResponses.error(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("Error inesperado al eliminar marca {}", id, e);
            return ApiResponses.error("Error al eliminar la marca");
        }
    }

    @PutMapping("/api/cambiar-estado/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> cambiarEstado(@PathVariable Long id) {
        try {
            return brandService.cambiarEstadoMarca(id)
                    .map(marca -> ResponseEntity.ok(Map.of(
                            "success", true,
                            "message", "Estado cambiado exitosamente",
                            "data", BrandResponse.from(marca)
                    )))
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                            "success", false,
                            "message", "Marca no encontrada"
                    )));
        } catch (Exception e) {
            log.error("Error al cambiar estado de marca {}", id, e);
            return ApiResponses.error("Error al cambiar el estado");
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
                return ApiResponses.error("La URL de la imagen es requerida", HttpStatus.BAD_REQUEST);
            }

            Brand actualizada = brandService.actualizarImagen(id, imagenUrl.trim());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Imagen actualizada exitosamente",
                    "data", BrandResponse.from(actualizada)
            ));

        } catch (BrandService.MarcaException e) {
            log.warn("Error al actualizar imagen de marca {}: {}", id, e.getMessage());
            return ApiResponses.error(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("Error inesperado al actualizar imagen de marca {}", id, e);
            return ApiResponses.error("Error al actualizar la imagen");
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
                    "data", BrandResponse.from(actualizada)
            ));

        } catch (BrandService.MarcaException e) {
            log.warn("Error al eliminar imagen de marca {}: {}", id, e.getMessage());
            return ApiResponses.error(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("Error inesperado al eliminar imagen de marca {}", id, e);
            return ApiResponses.error("Error al eliminar la imagen");
        }
    }

    // ===================== API REST - Búsquedas =====================

    @GetMapping("/api/buscar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> buscarMarcas(@RequestParam String q) {
        try {
            if (q == null || q.trim().isEmpty()) {
                return ApiResponses.error("Parámetro de búsqueda requerido", HttpStatus.BAD_REQUEST);
            }

            List<BrandResponse> marcas = brandService.buscarMarcas(q.trim()).stream()
                    .map(BrandResponse::from)
                    .toList();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", marcas,
                    "total", marcas.size()
            ));
        } catch (Exception e) {
            log.error("Error en búsqueda con query: {}", q, e);
            return ApiResponses.error("Error en la búsqueda");
        }
    }

    @GetMapping("/api/activas")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> marcasActivas() {
        try {
            List<BrandResponse> marcas = brandService.listarMarcas().stream()
                    .map(BrandResponse::from)
                    .toList();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", marcas
            ));
        } catch (Exception e) {
            log.error("Error al obtener marcas activas", e);
            return ApiResponses.error("Error al obtener marcas activas");
        }
    }

    @GetMapping("/api/con-imagen")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> marcasConImagen() {
        try {
            List<BrandResponse> marcas = brandService.listarMarcasConImagen().stream()
                    .map(BrandResponse::from)
                    .toList();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", marcas,
                    "total", marcas.size()
            ));
        } catch (Exception e) {
            log.error("Error al obtener marcas con imagen", e);
            return ApiResponses.error("Error al obtener marcas con imagen");
        }
    }

    @GetMapping("/api/sin-imagen")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> marcasSinImagen() {
        try {
            List<BrandResponse> marcas = brandService.listarMarcasSinImagen().stream()
                    .map(BrandResponse::from)
                    .toList();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", marcas,
                    "total", marcas.size()
            ));
        } catch (Exception e) {
            log.error("Error al obtener marcas sin imagen", e);
            return ApiResponses.error("Error al obtener marcas sin imagen");
        }
    }
}
