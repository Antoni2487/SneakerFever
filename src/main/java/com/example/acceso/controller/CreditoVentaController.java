package com.example.acceso.controller;

import com.example.acceso.dto.*;
import com.example.acceso.model.Usuario;
import com.example.acceso.service.CreditoVentaService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
@RequestMapping("/creditos")
@RequiredArgsConstructor
public class CreditoVentaController {

    private final CreditoVentaService creditoVentaService;


    // ===================== VISTAS =====================

    /**
     * Página principal de gestión de créditos
     */
    @GetMapping({"", "/"})
    public String listarCreditos(Model model) {
        model.addAttribute("title", "Gestión de Créditos");
        return "admin/creditos";
    }

    // ===================== API REST - CONSULTAS DE CRÉDITOS =====================

    /**
     * Obtener crédito por ID
     */
    @GetMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> obtenerCredito(@PathVariable Long id) {
        try {
            CreditoVentaResponse credito = creditoVentaService.obtenerCreditoPorId(id);
            return createSuccessResponse(credito);
        } catch (RuntimeException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return createErrorResponse("Error al obtener el crédito: " + e.getMessage());
        }
    }

    /**
     * Obtener crédito por ID de venta
     */
    @GetMapping("/api/venta/{ventaId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> obtenerCreditoPorVenta(@PathVariable Long ventaId) {
        try {
            CreditoVentaResponse credito = creditoVentaService.obtenerCreditoPorVentaId(ventaId);
            return createSuccessResponse(credito);
        } catch (RuntimeException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return createErrorResponse("Error al obtener el crédito: " + e.getMessage());
        }
    }

    /**
     * Listar créditos por cliente
     */
    @GetMapping("/api/cliente/{clienteId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> listarCreditosPorCliente(@PathVariable Long clienteId) {
        try {
            List<CreditoVentaResponse> creditos = creditoVentaService.listarCreditosPorCliente(clienteId);
            return createSuccessListResponse(creditos);
        } catch (Exception e) {
            return createErrorResponse("Error al listar créditos del cliente: " + e.getMessage());
        }
    }

    /**
     * Listar créditos activos
     */
    @GetMapping("/api/activos")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> listarCreditosActivos() {
        try {
            List<CreditoVentaResponse> creditos = creditoVentaService.listarCreditosActivos();
            return createSuccessListResponse(creditos);
        } catch (Exception e) {
            return createErrorResponse("Error al listar créditos activos: " + e.getMessage());
        }
    }

    /**
     * Listar créditos vencidos
     */
    @GetMapping("/api/vencidos")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> listarCreditosVencidos() {
        try {
            List<CreditoVentaResponse> creditos = creditoVentaService.listarCreditosVencidos();
            return createSuccessListResponse(creditos);
        } catch (Exception e) {
            return createErrorResponse("Error al listar créditos vencidos: " + e.getMessage());
        }
    }

    /**
     * Listar créditos próximos a vencer
     */
    @GetMapping("/api/proximos-vencer")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> listarCreditosProximosVencer(
            @RequestParam(defaultValue = "7") int dias) {
        try {
            List<CreditoVentaResponse> creditos = creditoVentaService.listarCreditosProximosVencer(dias);
            return createSuccessListResponse(creditos);
        } catch (Exception e) {
            return createErrorResponse("Error al listar créditos próximos a vencer: " + e.getMessage());
        }
    }

    // ===================== API REST - CUOTAS =====================

    /**
     * Listar cuotas de un crédito
     */
    @GetMapping("/api/{creditoId}/cuotas")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> listarCuotasPorCredito(@PathVariable Long creditoId) {
        try {
            List<CuotaPagoResponse> cuotas = creditoVentaService.listarCuotasPorCredito(creditoId);
            return createSuccessListResponse(cuotas);
        } catch (Exception e) {
            return createErrorResponse("Error al listar cuotas: " + e.getMessage());
        }
    }

    /**
     * Obtener detalle de una cuota
     */
    @GetMapping("/api/cuotas/{cuotaId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> obtenerCuota(@PathVariable Long cuotaId) {
        try {
            CuotaPagoResponse cuota = creditoVentaService.obtenerCuotaPorId(cuotaId);
            return createSuccessResponse(cuota);
        } catch (RuntimeException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return createErrorResponse("Error al obtener la cuota: " + e.getMessage());
        }
    }

    /**
     * Listar cuotas vencidas
     */
    @GetMapping("/api/cuotas/vencidas")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> listarCuotasVencidas() {
        try {
            List<CuotaPagoResponse> cuotas = creditoVentaService.listarCuotasVencidas();
            return createSuccessListResponse(cuotas);
        } catch (Exception e) {
            return createErrorResponse("Error al listar cuotas vencidas: " + e.getMessage());
        }
    }

    // ===================== API REST - PAGOS =====================

    /**
     * Registrar pago de una cuota
     */
    @PostMapping("/api/pagos/registrar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> registrarPago(
            @Valid @RequestBody RegistrarPagoRequest request,
            BindingResult result,
            HttpSession session) {

        if (result.hasErrors()) {
            return createValidationErrorResponse(result);
        }


        Usuario usuarioLogueado = (Usuario) session.getAttribute("usuarioLogueado");
        String nombreUsuario = (usuarioLogueado != null)
                ? usuarioLogueado.getUsuario()
                : "SYSTEM";

        try {
            RegistroPagoResponse pago = creditoVentaService.registrarPago(request, nombreUsuario);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Pago registrado exitosamente");
            response.put("data", pago);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (RuntimeException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return createErrorResponse("Error al registrar el pago: " + e.getMessage());
        }
    }


    /**
     * Listar pagos de un crédito
     */
    @GetMapping("/api/{creditoId}/pagos")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> listarPagosPorCredito(@PathVariable Long creditoId) {
        try {
            List<RegistroPagoResponse> pagos = creditoVentaService.listarPagosPorCredito(creditoId);
            return createSuccessListResponse(pagos);
        } catch (Exception e) {
            return createErrorResponse("Error al listar pagos: " + e.getMessage());
        }
    }




    // ===================== API REST - REPORTES =====================

    /**
     * Generar reporte de créditos
     */
    @GetMapping("/api/reporte")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> generarReporteCreditos() {
        try {
            ReporteCreditosDTO reporte = creditoVentaService.generarReporteCreditos();
            return createSuccessResponse(reporte);
        } catch (Exception e) {
            return createErrorResponse("Error al generar reporte: " + e.getMessage());
        }
    }

    // ===================== API REST - ACTUALIZACIÓN DE ESTADOS =====================

    /**
     * Actualizar estados de créditos
     */
    @PostMapping("/api/actualizar-estados")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> actualizarEstadosCreditos() {
        try {
            creditoVentaService.actualizarEstadosCreditos();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Estados de créditos actualizados exitosamente");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse("Error al actualizar estados: " + e.getMessage());
        }
    }

    /**
     * Actualizar estados de cuotas
     */
    @PostMapping("/api/actualizar-estados-cuotas")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> actualizarEstadosCuotas() {
        try {
            creditoVentaService.actualizarEstadosCuotas();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Estados de cuotas actualizados exitosamente");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse("Error al actualizar estados: " + e.getMessage());
        }
    }

    // ===================== MÉTODOS DE UTILIDAD =====================

    /**
     * Crear respuesta exitosa con un solo objeto
     */
    private ResponseEntity<Map<String, Object>> createSuccessResponse(Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", data);
        return ResponseEntity.ok(response);
    }

    /**
     * Crear respuesta exitosa con lista
     */
    private ResponseEntity<Map<String, Object>> createSuccessListResponse(List<?> data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", data);
        response.put("total", data.size());
        return ResponseEntity.ok(response);
    }

    /**
     * Crear respuesta de error estándar
     */
    private ResponseEntity<Map<String, Object>> createErrorResponse(String message) {
        return createErrorResponse(message, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Crear respuesta de error con status personalizado
     */
    private ResponseEntity<Map<String, Object>> createErrorResponse(String message, HttpStatus status) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return ResponseEntity.status(status).body(response);
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