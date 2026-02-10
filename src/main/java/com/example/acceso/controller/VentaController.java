package com.example.acceso.controller;

import com.example.acceso.dto.*;
import com.example.acceso.model.EntidadesVenta.ComprobanteSecuencia;
import com.example.acceso.model.EnumVentas.EstadoVenta;
import com.example.acceso.model.EnumVentas.TipoComprobante;
import com.example.acceso.model.Usuario;
import com.example.acceso.repository.RepositorioVentas.ComprobanteSecuenciaRepository;
import com.example.acceso.service.VentaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;


import java.time.LocalDateTime;
import java.util.*;


@Controller
@RequestMapping("/ventas")
public class VentaController {

    private final VentaService ventaService;
    private final ComprobanteSecuenciaRepository comprobanteSecuenciaRepository;

    public VentaController(VentaService ventaService,
                           ComprobanteSecuenciaRepository comprobanteSecuenciaRepository) {
        this.ventaService = ventaService;
        this.comprobanteSecuenciaRepository = comprobanteSecuenciaRepository;
    }


    // ===================== Vistas =====================

    /**
     * Página principal de gestión de ventas
     */
    @GetMapping({"", "/"})
    public String listarVentas(Model model) {
        model.addAttribute("title", "Gestión de Ventas");
        return "admin/ventas";
    }

    @GetMapping("/listar")
    public String listarVentasRedirect() {
        return "redirect:/ventas";
    }

    @GetMapping("/nueva")
    public String nuevaVenta(Model model) {
        model.addAttribute("title", "Nueva Venta");
        return "admin/ventas/nueva";
    }

    // ===================== API REST - Listados =====================

    /**
     * Obtener todas las ventas (JSON)
     */
    @GetMapping("/api/listar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> listarVentasJson() {
        try {
            List<VentaResponse> ventas = ventaService.listarTodasLasVentas();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", ventas);
            response.put("total", ventas.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse("Error al listar ventas: " + e.getMessage());
        }
    }

    /**
     * Obtener ventas por cliente
     */
    @GetMapping("/api/cliente/{clienteId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> listarVentasPorCliente(@PathVariable Long clienteId) {
        try {
            List<VentaResponse> ventas = ventaService.listarVentasPorCliente(clienteId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", ventas);
            response.put("total", ventas.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse("Error al listar ventas del cliente: " + e.getMessage());
        }
    }

    /**
     * Obtener ventas por estado
     */
    @GetMapping("/api/estado/{estado}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> listarVentasPorEstado(@PathVariable EstadoVenta estado) {
        try {
            List<VentaResponse> ventas = ventaService.listarVentasPorEstado(estado);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", ventas);
            response.put("total", ventas.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse("Error al listar ventas por estado: " + e.getMessage());
        }
    }

    /**
     * Obtener ventas por rango de fechas
     */
    @GetMapping("/api/rango-fechas")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> listarVentasPorRangoFechas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin) {
        try {
            List<VentaResponse> ventas = ventaService.listarVentasPorRangoFechas(fechaInicio, fechaFin);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", ventas);
            response.put("total", ventas.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse("Error al listar ventas por rango de fechas: " + e.getMessage());
        }
    }

    /**
     * Obtener ventas con crédito
     */
    @GetMapping("/api/creditos")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> listarVentasConCredito() {
        try {
            List<VentaResponse> ventas = ventaService.listarVentasConCredito();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", ventas);
            response.put("total", ventas.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse("Error al listar ventas con crédito: " + e.getMessage());
        }
    }

    // ===================== API REST - CRUD =====================

    /**
     * Obtener venta por ID
     */
    @GetMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> obtenerVenta(@PathVariable Long id) {
        try {
            VentaResponse venta = ventaService.obtenerVentaPorId(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", venta);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return createErrorResponse(e.getMessage());
        } catch (Exception e) {
            return createErrorResponse("Error al obtener la venta: " + e.getMessage());
        }
    }

    @PostMapping("/api/crear")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> crearVenta(
            @Valid @RequestBody CrearVentaRequest request,
            BindingResult result,
            HttpSession session) {

        if (result.hasErrors()) {
            return createValidationErrorResponse(result);
        }

        // Intentamos obtener el usuario logueado desde la sesión
        Usuario usuarioLogueado = (Usuario) session.getAttribute("usuarioLogueado");
        String nombreUsuario = (usuarioLogueado != null)
                ? usuarioLogueado.getUsuario()
                : "SYSTEM"; // fallback si no hay sesión activa

        try {
            VentaResponse nuevaVenta = ventaService.crearVenta(request, nombreUsuario);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Venta creada exitosamente");
            response.put("data", nuevaVenta);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (RuntimeException e) {
            return createErrorResponse(e.getMessage());
        } catch (Exception e) {
            return createErrorResponse("Error al crear la venta: " + e.getMessage());
        }
    }
    @GetMapping("/api/series/activas")
        @ResponseBody
        public ResponseEntity<Map<String, Object>> obtenerSerieActiva(
                @RequestParam("tipo_comprobante") TipoComprobante tipoComprobante) {
            try {
                // Busca la primera serie activa del tipo solicitado
                Optional<ComprobanteSecuencia> serieOpt = comprobanteSecuenciaRepository
                        .findAll()
                        .stream()
                        .filter(c -> Boolean.TRUE.equals(c.getActivo()) && c.getTipoComprobante() == tipoComprobante)
                        .findFirst();

                if (serieOpt.isEmpty()) {
                    Map<String, Object> error = new HashMap<>();
                    error.put("success", false);
                    error.put("message", "No hay series activas para " + tipoComprobante);
                    return ResponseEntity.ok(error);
                }

                ComprobanteSecuencia serie = serieOpt.get();

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("data", Map.of(
                        "serie", serie.getSerie(),
                        "numero_actual", serie.getNumeroActual()
                ));

                return ResponseEntity.ok(response);

            } catch (Exception e) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Error al obtener serie activa: " + e.getMessage());
                return ResponseEntity.internalServerError().body(error);
            }
        }


    /**
     * Actualizar estado de venta
     */
    @PutMapping("/api/estado/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> actualizarEstadoVenta(
            @PathVariable Long id,
            @RequestParam EstadoVenta nuevoEstado) {
        try {
            VentaResponse ventaActualizada = ventaService.actualizarEstadoVenta(id, nuevoEstado);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Estado actualizado exitosamente");
            response.put("data", ventaActualizada);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return createErrorResponse(e.getMessage());
        } catch (Exception e) {
            return createErrorResponse("Error al actualizar el estado: " + e.getMessage());
        }
    }

    /**
     * Anular venta
     */
    @PutMapping("/api/anular/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> anularVenta(@PathVariable Long id) {
        try {
            VentaResponse ventaAnulada = ventaService.anularVenta(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Venta anulada exitosamente");
            response.put("data", ventaAnulada);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return createErrorResponse(e.getMessage());
        } catch (Exception e) {
            return createErrorResponse("Error al anular la venta: " + e.getMessage());
        }
    }

    // ===================== API REST - Reportes =====================

    /**
     * Generar reporte de ventas
     */
    @GetMapping("/api/reporte")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> generarReporteVentas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin) {
        try {
            ReporteVentasDTO reporte = ventaService.generarReporteVentas(fechaInicio, fechaFin);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", reporte);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse("Error al generar reporte: " + e.getMessage());
        }
    }

    /**
     * Obtener estadísticas del dashboard de ventas
     */
    @GetMapping("/api/estadisticas")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> obtenerEstadisticasVentas() {
        try {
            Map<String, Object> estadisticas = ventaService.obtenerEstadisticasDashboard();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", estadisticas);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse("Error al obtener estadísticas: " + e.getMessage());
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
