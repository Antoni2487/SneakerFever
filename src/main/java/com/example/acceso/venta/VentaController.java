package com.example.acceso.venta;

import org.springframework.data.domain.Sort;
import com.example.acceso.common.Paginacion;
import org.springframework.security.access.prepost.PreAuthorize;
import com.example.acceso.seguridad.Permisos;
import com.example.acceso.usuario.Usuario;
import com.example.acceso.dashboard.ReporteVentasDTO;
import com.example.acceso.common.ApiResponses;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;


import java.time.LocalDateTime;
import java.util.*;


@RestController
@RequestMapping("/ventas")
@PreAuthorize("hasAuthority('" + Permisos.VENTAS + "')")
public class VentaController {

    private final VentaService ventaService;
    private final ComprobanteSecuenciaService comprobanteSecuenciaService;

    public VentaController(VentaService ventaService,
                           ComprobanteSecuenciaService comprobanteSecuenciaService) {
        this.ventaService = ventaService;
        this.comprobanteSecuenciaService = comprobanteSecuenciaService;
    }



    // ===================== API REST - Listados =====================

    /**
     * Obtener todas las ventas (JSON)
     */
    /**
     * Listado paginado del panel: {@code ?page=0&size=10&q=texto}. Busca por cliente (nombre o documento) o comprobante; más recientes primero.
     */
    @GetMapping("/api/pagina")
    public ResponseEntity<Map<String, Object>> paginarVentas(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + Paginacion.TAMANO_POR_DEFECTO) int size,
            @RequestParam(required = false) String q) {
        return Paginacion.respuesta(ventaService.paginarVentas(Paginacion.busqueda(q),
                Paginacion.crear(page, size, Sort.by(Sort.Direction.DESC, "fechaCreacion").and(Sort.by(Sort.Direction.DESC, "id")))));
    }

    @GetMapping("/api/listar")
    public ResponseEntity<Map<String, Object>> listarVentasJson() {
        try {
            List<VentaResponse> ventas = ventaService.listarTodasLasVentas();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", ventas);
            response.put("total", ventas.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ApiResponses.error("Error al listar ventas: " + ApiResponses.mensajeSeguro(e));
        }
    }

    /**
     * Obtener ventas por cliente
     */
    @GetMapping("/api/cliente/{clienteId}")
    public ResponseEntity<Map<String, Object>> listarVentasPorCliente(@PathVariable Long clienteId) {
        try {
            List<VentaResponse> ventas = ventaService.listarVentasPorCliente(clienteId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", ventas);
            response.put("total", ventas.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ApiResponses.error("Error al listar ventas del cliente: " + ApiResponses.mensajeSeguro(e));
        }
    }

    /**
     * Obtener ventas por estado
     */
    @GetMapping("/api/estado/{estado}")
    public ResponseEntity<Map<String, Object>> listarVentasPorEstado(@PathVariable EstadoVenta estado) {
        try {
            List<VentaResponse> ventas = ventaService.listarVentasPorEstado(estado);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", ventas);
            response.put("total", ventas.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ApiResponses.error("Error al listar ventas por estado: " + ApiResponses.mensajeSeguro(e));
        }
    }

    /**
     * Obtener ventas por rango de fechas
     */
    @GetMapping("/api/rango-fechas")
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
            return ApiResponses.error("Error al listar ventas por rango de fechas: " + ApiResponses.mensajeSeguro(e));
        }
    }

    /**
     * Obtener ventas con crédito
     */
    @GetMapping("/api/creditos")
    public ResponseEntity<Map<String, Object>> listarVentasConCredito() {
        try {
            List<VentaResponse> ventas = ventaService.listarVentasConCredito();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", ventas);
            response.put("total", ventas.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ApiResponses.error("Error al listar ventas con crédito: " + ApiResponses.mensajeSeguro(e));
        }
    }

    // ===================== API REST - CRUD =====================

    /**
     * Obtener venta por ID
     */
    @GetMapping("/api/{id}")
    public ResponseEntity<Map<String, Object>> obtenerVenta(@PathVariable Long id) {
        try {
            VentaResponse venta = ventaService.obtenerVentaPorId(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", venta);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ApiResponses.error(ApiResponses.mensajeSeguro(e));
        } catch (Exception e) {
            return ApiResponses.error("Error al obtener la venta: " + ApiResponses.mensajeSeguro(e));
        }
    }

    @PostMapping("/api/crear")
    public ResponseEntity<Map<String, Object>> crearVenta(
            @Valid @RequestBody CrearVentaRequest request,
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
            VentaResponse nuevaVenta = ventaService.crearVenta(request, nombreUsuario);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Venta creada exitosamente");
            response.put("data", nuevaVenta);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (RuntimeException e) {
            return ApiResponses.error(ApiResponses.mensajeSeguro(e));
        } catch (Exception e) {
            return ApiResponses.error("Error al crear la venta: " + ApiResponses.mensajeSeguro(e));
        }
    }
    @GetMapping("/api/series/activas")
    public ResponseEntity<Map<String, Object>> obtenerSerieActiva(
            @RequestParam("tipo_comprobante") TipoComprobante tipoComprobante) {
        try {
            Optional<ComprobanteSecuencia> serieOpt = comprobanteSecuenciaService.obtenerSerieActiva(tipoComprobante);

            if (serieOpt.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "No hay series activas para " + tipoComprobante
                ));
            }

            ComprobanteSecuencia serie = serieOpt.get();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", Map.of(
                            "serie", serie.getSerie(),
                            "numero_actual", serie.getNumeroActual()
                    )
            ));

        } catch (Exception e) {
            return ApiResponses.error("Error al obtener serie activa: " + ApiResponses.mensajeSeguro(e));
        }
    }


    /**
     * Actualizar estado de venta
     */
    @PutMapping("/api/estado/{id}")
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
            return ApiResponses.error(ApiResponses.mensajeSeguro(e));
        } catch (Exception e) {
            return ApiResponses.error("Error al actualizar el estado: " + ApiResponses.mensajeSeguro(e));
        }
    }

    /**
     * Anular venta
     */
    @PutMapping("/api/anular/{id}")
    public ResponseEntity<Map<String, Object>> anularVenta(@PathVariable Long id,
            @AuthenticationPrincipal Usuario usuarioLogueado) {
        try {
            VentaResponse ventaAnulada = ventaService.anularVenta(id, usuarioLogueado.getUsuario());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Venta anulada exitosamente");
            response.put("data", ventaAnulada);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ApiResponses.error(ApiResponses.mensajeSeguro(e));
        } catch (Exception e) {
            return ApiResponses.error("Error al anular la venta: " + ApiResponses.mensajeSeguro(e));
        }
    }

    // ===================== API REST - Reportes =====================

    /**
     * Generar reporte de ventas
     */
    @GetMapping("/api/reporte")
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
            return ApiResponses.error("Error al generar reporte: " + ApiResponses.mensajeSeguro(e));
        }
    }

    /**
     * Obtener estadísticas del dashboard de ventas
     */
    @GetMapping("/api/estadisticas")
    public ResponseEntity<Map<String, Object>> obtenerEstadisticasVentas() {
        try {
            Map<String, Object> estadisticas = ventaService.obtenerEstadisticasDashboard();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", estadisticas);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ApiResponses.error("Error al obtener estadísticas: " + ApiResponses.mensajeSeguro(e));
        }
    }

    // ===================== Métodos de utilidad =====================

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
