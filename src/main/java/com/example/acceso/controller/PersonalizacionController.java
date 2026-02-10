package com.example.acceso.controller;

import com.example.acceso.model.Brand;
import com.example.acceso.model.Personalizacion;
import com.example.acceso.service.BrandService;
import com.example.acceso.service.PersonalizacionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
@RequestMapping("/personalizacion")
public class PersonalizacionController {

    private static final Logger log = LoggerFactory.getLogger(PersonalizacionController.class);

    private final PersonalizacionService personalizacionService;
    private final BrandService brandService;

    public PersonalizacionController(PersonalizacionService personalizacionService,
                                      BrandService brandService) {
        this.personalizacionService = personalizacionService;
        this.brandService = brandService;
    }

    // ===================== Vistas =====================

    @GetMapping({"", "/"})
    public String mostrarPersonalizacion(Model model) {
        model.addAttribute("title", "Centro de Personalización");
        model.addAttribute("logoVacio", personalizacionService.logoEstaVacio());
        model.addAttribute("slidesConImagen", personalizacionService.contarSlidesConImagen());
        return "admin/personalizacion";
    }

    // ===================== API REST - Logo =====================

    @GetMapping("/api/logo")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> obtenerLogo() {
        try {
            Optional<Personalizacion> logo = personalizacionService.obtenerLogo();

            if (logo.isPresent()) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", logo.get()
                ));
            }

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "success", false,
                "message", "Logo no encontrado"
            ));
        } catch (Exception e) {
            log.error("Error al obtener logo", e);
            return createInternalErrorResponse("Error al obtener el logo");
        }
    }

    @PutMapping("/api/logo/imagen")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> actualizarImagenLogo(
            @RequestParam("imagenUrl") String imagenUrl) {
        try {
            if (imagenUrl == null || imagenUrl.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "La URL de la imagen es requerida"
                ));
            }

            Personalizacion actualizado = personalizacionService.actualizarLogo(imagenUrl.trim());

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Logo actualizado exitosamente",
                "data", actualizado
            ));

        } catch (PersonalizacionService.PersonalizacionException e) {
            log.warn("Error al actualizar logo: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error inesperado al actualizar logo", e);
            return createInternalErrorResponse("Error al actualizar el logo");
        }
    }

    @DeleteMapping("/api/logo/imagen")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> eliminarImagenLogo() {
        try {
            Personalizacion actualizado = personalizacionService.eliminarLogo();

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Logo eliminado exitosamente",
                "data", actualizado
            ));

        } catch (PersonalizacionService.PersonalizacionException e) {
            log.warn("Error al eliminar logo: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error inesperado al eliminar logo", e);
            return createInternalErrorResponse("Error al eliminar el logo");
        }
    }

    // ===================== API REST - Slides (Listados) =====================

    @GetMapping("/api/slides")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> listarSlides() {
        try {
            List<Personalizacion> slides = personalizacionService.listarSlides();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", slides,
                "total", slides.size()
            ));
        } catch (Exception e) {
            log.error("Error al listar slides", e);
            return createInternalErrorResponse("Error al listar slides");
        }
    }

    @GetMapping("/api/slides/con-marca")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> listarSlidesConMarca() {
        try {
            List<Personalizacion> slides = personalizacionService.listarSlidesConMarca();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", slides,
                "total", slides.size()
            ));
        } catch (Exception e) {
            log.error("Error al listar slides con marca", e);
            return createInternalErrorResponse("Error al listar slides con marca");
        }
    }

    @GetMapping("/api/slides/sin-marca")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> listarSlidesSinMarca() {
        try {
            List<Personalizacion> slides = personalizacionService.obtenerSlidesSinMarca();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", slides,
                "total", slides.size()
            ));
        } catch (Exception e) {
            log.error("Error al listar slides sin marca", e);
            return createInternalErrorResponse("Error al listar slides sin marca");
        }
    }

    // ===================== API REST - Slides (Individual) =====================

    @GetMapping("/api/slide/{orden}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> obtenerSlidePorOrden(@PathVariable Integer orden) {
        try {
            ValidationResult validacion = validarOrden(orden);
            if (!validacion.isValid()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", validacion.getMessage()
                ));
            }

            Optional<Personalizacion> slide = personalizacionService.obtenerSlidePorOrden(orden);

            if (slide.isPresent()) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", slide.get()
                ));
            }

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "success", false,
                "message", "Slide no encontrado"
            ));
        } catch (Exception e) {
            log.error("Error al obtener slide con orden: {}", orden, e);
            return createInternalErrorResponse("Error al obtener el slide");
        }
    }

    @PutMapping("/api/slide/{orden}/imagen")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> actualizarImagenSlide(
            @PathVariable Integer orden,
            @RequestParam("imagenUrl") String imagenUrl,
            @RequestParam(value = "marcaId", required = false) Long marcaId) {
        try {
            ValidationResult validacion = validarOrden(orden);
            if (!validacion.isValid()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", validacion.getMessage()
                ));
            }

            if (imagenUrl == null || imagenUrl.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "La URL de la imagen es requerida"
                ));
            }

            Personalizacion actualizado = personalizacionService.actualizarSlide(
                orden,
                imagenUrl.trim(),
                marcaId
            );

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Slide actualizado exitosamente",
                "data", actualizado
            ));

        } catch (PersonalizacionService.PersonalizacionException e) {
            log.warn("Error al actualizar slide {}: {}", orden, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error inesperado al actualizar slide {}", orden, e);
            return createInternalErrorResponse("Error al actualizar el slide");
        }
    }

    @PutMapping("/api/slide/{orden}/marca")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> actualizarMarcaSlide(
            @PathVariable Integer orden,
            @RequestParam(value = "marcaId", required = false) Long marcaId) {
        try {
            ValidationResult validacion = validarOrden(orden);
            if (!validacion.isValid()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", validacion.getMessage()
                ));
            }

            Personalizacion actualizado = personalizacionService.actualizarMarcaSlide(orden, marcaId);

            String mensaje = marcaId != null
                ? "Marca asignada exitosamente"
                : "Marca removida exitosamente";

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", mensaje,
                "data", actualizado
            ));

        } catch (PersonalizacionService.PersonalizacionException e) {
            log.warn("Error al actualizar marca del slide {}: {}", orden, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error inesperado al actualizar marca del slide {}", orden, e);
            return createInternalErrorResponse("Error al actualizar la marca del slide");
        }
    }

    @DeleteMapping("/api/slide/{orden}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> limpiarSlide(@PathVariable Integer orden) {
        try {
            ValidationResult validacion = validarOrden(orden);
            if (!validacion.isValid()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", validacion.getMessage()
                ));
            }

            Personalizacion actualizado = personalizacionService.limpiarSlide(orden);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Slide limpiado exitosamente",
                "data", actualizado
            ));

        } catch (PersonalizacionService.PersonalizacionException e) {
            log.warn("Error al limpiar slide {}: {}", orden, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error inesperado al limpiar slide {}", orden, e);
            return createInternalErrorResponse("Error al limpiar el slide");
        }
    }

    // ===================== API REST - Búsquedas =====================

    @GetMapping("/api/slides/por-marca/{marcaId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> obtenerSlidesPorMarca(@PathVariable Long marcaId) {
        try {
            if (marcaId == null || marcaId <= 0) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "ID de marca inválido"
                ));
            }

            List<Personalizacion> slides = personalizacionService.obtenerSlidesPorMarca(marcaId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", slides,
                "total", slides.size()
            ));
        } catch (PersonalizacionService.PersonalizacionException e) {
            log.warn("Error al buscar slides por marca {}: {}", marcaId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error al buscar slides por marca {}", marcaId, e);
            return createInternalErrorResponse("Error al buscar slides por marca");
        }
    }

    // ===================== API REST - Endpoints Públicos =====================

    @GetMapping("/api/public/configuracion")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> obtenerConfiguracionPublica() {
        try {
            Optional<Personalizacion> logo = personalizacionService.obtenerLogo();
            List<Personalizacion> slides = personalizacionService.listarSlidesConMarca();

            // Filtrar solo slides con imagen
            List<Personalizacion> slidesActivos = slides.stream()
                .filter(s -> s.getImagenUrl() != null && !s.getImagenUrl().trim().isEmpty())
                .toList();

            Map<String, Object> configuracion = new HashMap<>();
            configuracion.put("logo", logo.orElse(null));
            configuracion.put("slides", slidesActivos);
            configuracion.put("totalSlides", slidesActivos.size());

            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", configuracion
            ));
        } catch (Exception e) {
            log.error("Error al obtener configuración pública", e);
            return createInternalErrorResponse("Error al obtener la configuración");
        }
    }

    @GetMapping("/api/public/logo")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> obtenerLogoPublico() {
        try {
            Optional<String> logoUrl = personalizacionService.obtenerImagenUrlLogo();

            if (logoUrl.isPresent()) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "url", logoUrl.get()
                ));
            }

            return ResponseEntity.ok(Map.of(
                "success", true,
                "url", null,
                "message", "No hay logo configurado"
            ));
        } catch (Exception e) {
            log.error("Error al obtener logo público", e);
            return createInternalErrorResponse("Error al obtener el logo");
        }
    }

    @GetMapping("/api/public/slides")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> obtenerSlidesPublicos() {
        try {
            List<Personalizacion> slides = personalizacionService.listarSlidesConMarca();

            // Filtrar solo slides con imagen
            List<Personalizacion> slidesActivos = slides.stream()
                .filter(s -> s.getImagenUrl() != null && !s.getImagenUrl().trim().isEmpty())
                .toList();

            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", slidesActivos,
                "total", slidesActivos.size()
            ));
        } catch (Exception e) {
            log.error("Error al obtener slides públicos", e);
            return createInternalErrorResponse("Error al obtener los slides");
        }
    }

    // ===================== API REST - Utilidades =====================

    @GetMapping("/api/marcas-disponibles")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> obtenerMarcasDisponibles() {
        try {
            List<Brand> marcas = brandService.listarMarcas();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", marcas,
                "total", marcas.size()
            ));
        } catch (Exception e) {
            log.error("Error al obtener marcas disponibles", e);
            return createInternalErrorResponse("Error al obtener marcas disponibles");
        }
    }

    // ===================== Métodos de Validación =====================

    private ValidationResult validarOrden(Integer orden) {
        if (orden == null) {
            return ValidationResult.invalid("El orden es requerido");
        }

        if (orden < 1 || orden > 5) {
            return ValidationResult.invalid("El orden debe estar entre 1 y 5");
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