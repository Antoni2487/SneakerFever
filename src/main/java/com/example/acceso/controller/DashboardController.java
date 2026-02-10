package com.example.acceso.controller;

import com.example.acceso.dto.ReporteVentasDTO;
import com.example.acceso.model.Product;
import com.example.acceso.service.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
public class DashboardController {

    private final UsuarioService usuarioService;
    private final VentaService ventaService;
    private final ClienteService clienteService;
    private final ProductService productService;

    public DashboardController(UsuarioService usuarioService,
                               VentaService ventaService,
                               ClienteService clienteService,
                               ProductService productService) {
        this.usuarioService = usuarioService;
        this.ventaService = ventaService;
        this.clienteService = clienteService;
        this.productService = productService;
    }

    // ===================== VISTA PRINCIPAL =====================
    @GetMapping
    public String mostrarDashboard(Model model) {
        long totalUsuarios = usuarioService.contarUsuarios();
        model.addAttribute("totalUsuarios", totalUsuarios);
        return "admin/index"; // tu plantilla dashboard.html o index.html
    }

    // ===================== API - ESTADÍSTICAS =====================
    @GetMapping("/api/estadisticas")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> obtenerEstadisticas() {
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Object> estadisticas = ventaService.obtenerEstadisticasDashboard();
            response.put("success", true);
            response.put("data", estadisticas);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al obtener estadísticas: " + e.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    // ===================== API - REPORTE DE VENTAS =====================
    @GetMapping("/api/reporte")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> generarReporte(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin) {

        Map<String, Object> response = new HashMap<>();
        try {
            ReporteVentasDTO reporte = ventaService.generarReporteVentas(fechaInicio, fechaFin);
            response.put("success", true);
            response.put("data", reporte);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al generar reporte: " + e.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    // ===================== API - CLIENTES =====================
    @GetMapping("/api/count-clientes")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> contarClientes() {
        Map<String, Object> response = new HashMap<>();
        try {
            long total = clienteService.contarClientes();
            response.put("success", true);
            response.put("total", total);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al contar clientes: " + e.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    // ===================== API - PRODUCTOS =====================
    @GetMapping("/api/listar-productos")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> listarProductos() {
        Map<String, Object> response = new HashMap<>();
        try {
            List<Product> productos = productService.listarProductos();
            response.put("success", true);
            response.put("data", productos);
            response.put("total", productos.size());
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al listar productos: " + e.getMessage());
        }
        return ResponseEntity.ok(response);
    }
    
}
