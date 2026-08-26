package com.example.acceso.dashboard;

import com.example.acceso.product.Product;
import com.example.acceso.product.ProductService;
import com.example.acceso.cliente.ClienteService;
import com.example.acceso.usuario.UsuarioService;
import com.example.acceso.venta.CreditoVentaService;
import com.example.acceso.venta.VentaService;
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
    private final CreditoVentaService creditoVentaService;

    public DashboardController(UsuarioService usuarioService,
                               VentaService ventaService,
                               ClienteService clienteService,
                               ProductService productService,
                               CreditoVentaService creditoVentaService) {
        this.usuarioService = usuarioService;
        this.ventaService = ventaService;
        this.clienteService = clienteService;
        this.productService = productService;
        this.creditoVentaService = creditoVentaService;
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

    // ===================== API - RESUMEN CONSOLIDADO =====================

    /**
     * Resumen único para la carga inicial del dashboard: evita múltiples round-trips
     * combinando conteos/estadísticas ya expuestos por separado en cada dominio.
     */
    @GetMapping("/api/resumen")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> obtenerResumen() {
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Object> resumen = new HashMap<>();
            resumen.put("totalUsuarios", usuarioService.contarUsuarios());
            resumen.put("clientes", clienteService.obtenerEstadisticas());
            resumen.put("productos", productService.obtenerEstadisticas());
            resumen.put("ventas", ventaService.obtenerEstadisticasDashboard());
            resumen.put("creditos", creditoVentaService.generarReporteCreditos());
            resumen.put("stockBajo", productService.listarStockBajo(10).size());

            response.put("success", true);
            response.put("data", resumen);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al obtener el resumen: " + e.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    // ===================== API - TENDENCIA DE VENTAS =====================

    @GetMapping("/api/ventas-tendencia")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> obtenerVentasTendencia(
            @RequestParam(defaultValue = "7") int dias) {
        Map<String, Object> response = new HashMap<>();
        try {
            response.put("success", true);
            response.put("data", ventaService.obtenerVentasPorDia(dias));
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al obtener la tendencia de ventas: " + e.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    // ===================== API - PRODUCTOS MÁS VENDIDOS =====================

    @GetMapping("/api/productos-mas-vendidos")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> obtenerProductosMasVendidos(
            @RequestParam(defaultValue = "5") int limite) {
        Map<String, Object> response = new HashMap<>();
        try {
            response.put("success", true);
            response.put("data", ventaService.obtenerProductosMasVendidos(limite));
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al obtener los productos más vendidos: " + e.getMessage());
        }
        return ResponseEntity.ok(response);
    }

}
