package com.example.acceso.controller;

import com.example.acceso.dto.MovimientoInventarioResponse;
import com.example.acceso.dto.RegistrarMovimientoRequest;
import com.example.acceso.model.Usuario;
import com.example.acceso.model.EnumInventario.TipoMovimiento;
import com.example.acceso.service.MovimientoInventarioService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/inventario")
@RequiredArgsConstructor
public class MovimientoInventarioController {

    private final MovimientoInventarioService movimientoService;

    /**
     * Vista principal
     */
    @GetMapping({"", "/"})
    public String index(Model model) {
        model.addAttribute("title", "Gestión de Inventario");
        return "admin/inventario";
    }

    @PostMapping("/api/registrar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> registrarMovimiento(
            @Valid @RequestBody RegistrarMovimientoRequest request,
            BindingResult result,
            HttpSession session) {

        if (result.hasErrors()) {
            return createValidationErrorResponse(result);
        }

        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null) {
            return createErrorResponse("Usuario no autenticado", HttpStatus.UNAUTHORIZED);
        }

        try {
            MovimientoInventarioResponse movimiento = movimientoService.registrarMovimiento(request, usuario.getId());
            return createSuccessResponse("Movimiento registrado correctamente", movimiento);
        } catch (RuntimeException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return createErrorResponse("Error al registrar movimiento: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }



     @GetMapping("/api/ultimos") // ✅ corregido
    @ResponseBody
    public ResponseEntity<Map<String, Object>> listarUltimos() {
        try {
            List<MovimientoInventarioResponse> movimientos = movimientoService.listarUltimos();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", movimientos);
            response.put("total", movimientos.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse("Error al listar movimientos: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    /**
     * Kardex de producto
     */
    @GetMapping("/api/kardex/{productoId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> obtenerKardex(@PathVariable Long productoId) {
        try {
            List<MovimientoInventarioResponse> kardex = movimientoService.obtenerKardex(productoId);
            return createSuccessListResponse(kardex);
        } catch (Exception e) {
            return createErrorResponse("Error al obtener kardex: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Listar por tipo de movimiento
     */
    @GetMapping("/api/tipo/{tipo}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> listarPorTipo(@PathVariable TipoMovimiento tipo) {
        try {
            List<MovimientoInventarioResponse> movimientos = movimientoService.listarPorTipo(tipo);
            return createSuccessListResponse(movimientos);
        } catch (Exception e) {
            return createErrorResponse("Error al listar por tipo: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Listar por rango de fechas
     */
    @GetMapping("/api/fechas")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> listarPorFechas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin) {
        try {
            List<MovimientoInventarioResponse> movimientos = movimientoService.listarPorFechas(fechaInicio, fechaFin);
            return createSuccessListResponse(movimientos);
        } catch (Exception e) {
            return createErrorResponse("Error al listar por fechas: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ===================== UTILIDADES =====================

    private ResponseEntity<Map<String, Object>> createSuccessResponse(String message, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        response.put("data", data);
        return ResponseEntity.ok(response);
    }

    private ResponseEntity<Map<String, Object>> createSuccessListResponse(List<?> data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", data);
        response.put("total", data.size());
        return ResponseEntity.ok(response);
    }

    private ResponseEntity<Map<String, Object>> createErrorResponse(String message, HttpStatus status) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return ResponseEntity.status(status).body(response);
    }

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