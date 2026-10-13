package com.example.acceso.venta;

import org.springframework.security.access.prepost.PreAuthorize;
import com.example.acceso.seguridad.Permisos;
import com.example.acceso.usuario.Usuario;
import com.example.acceso.dashboard.ReporteCreditosDTO;
import com.example.acceso.common.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
@RequestMapping("/creditos")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('" + Permisos.VENTAS + "')")
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
            return ApiResponses.error(ApiResponses.mensajeSeguro(e), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return ApiResponses.error("Error al obtener el crédito: " + ApiResponses.mensajeSeguro(e));
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
            return ApiResponses.error(ApiResponses.mensajeSeguro(e), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return ApiResponses.error("Error al obtener el crédito: " + ApiResponses.mensajeSeguro(e));
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
            return ApiResponses.error("Error al listar créditos del cliente: " + ApiResponses.mensajeSeguro(e));
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
            return ApiResponses.error("Error al listar créditos activos: " + ApiResponses.mensajeSeguro(e));
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
            return ApiResponses.error("Error al listar créditos vencidos: " + ApiResponses.mensajeSeguro(e));
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
            return ApiResponses.error("Error al listar créditos próximos a vencer: " + ApiResponses.mensajeSeguro(e));
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
            return ApiResponses.error("Error al listar cuotas: " + ApiResponses.mensajeSeguro(e));
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
            return ApiResponses.error(ApiResponses.mensajeSeguro(e), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return ApiResponses.error("Error al obtener la cuota: " + ApiResponses.mensajeSeguro(e));
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
            return ApiResponses.error("Error al listar cuotas vencidas: " + ApiResponses.mensajeSeguro(e));
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
            @AuthenticationPrincipal Usuario usuarioLogueado) {

        if (result.hasErrors()) {
            return createValidationErrorResponse(result);
        }


        // Del JWT (antes HttpSession, que con el login de React siempre venía vacío
        // y caía en "SYSTEM": la venta fallaba al buscar ese usuario). SecurityConfig
        // exige autoridad en esta ruta, así que el principal siempre está presente.
        String nombreUsuario = usuarioLogueado.getUsuario();

        try {
            RegistroPagoResponse pago = creditoVentaService.registrarPago(request, nombreUsuario);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Pago registrado exitosamente");
            response.put("data", pago);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (RuntimeException e) {
            return ApiResponses.error(ApiResponses.mensajeSeguro(e), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return ApiResponses.error("Error al registrar el pago: " + ApiResponses.mensajeSeguro(e));
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
            return ApiResponses.error("Error al listar pagos: " + ApiResponses.mensajeSeguro(e));
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
            return ApiResponses.error("Error al generar reporte: " + ApiResponses.mensajeSeguro(e));
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
            return ApiResponses.error("Error al actualizar estados: " + ApiResponses.mensajeSeguro(e));
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
            return ApiResponses.error("Error al actualizar estados: " + ApiResponses.mensajeSeguro(e));
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