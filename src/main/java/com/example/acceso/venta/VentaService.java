package com.example.acceso.venta;

import com.example.acceso.product.Product;
import com.example.acceso.product.ProductRepository;
import com.example.acceso.cliente.Cliente;
import com.example.acceso.cliente.ClienteService;
import com.example.acceso.cliente.ClienteRepository;
import com.example.acceso.usuario.Usuario;
import com.example.acceso.usuario.UsuarioRepository;
import com.example.acceso.inventario.MotivoMovimiento;
import com.example.acceso.inventario.TipoMovimiento;
import com.example.acceso.inventario.TipoReferencia;
import com.example.acceso.inventario.MovimientoInventarioService;
import com.example.acceso.inventario.RegistrarMovimientoRequest;
import com.example.acceso.dashboard.ReporteVentasDTO;
import com.example.acceso.dashboard.ProductoMasVendidoDTO;
import com.example.acceso.dashboard.VentaPorDiaDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VentaService {

    private final VentaRepository ventaRepository;
    private final CreditoVentaService creditoVentaService;
    private final ComprobanteSecuenciaService comprobanteSecuenciaService;
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProductRepository productRepository;
    private final ClienteService clienteService;
    private final MovimientoInventarioService movimientoInventarioService;
    private final DetalleVentaRepository detalleVentaRepository;

    // ===================== CREAR VENTA =====================

    @Transactional
    public VentaResponse crearVenta(CrearVentaRequest request, String usuarioCreacion) {
        log.info("Iniciando creación de venta - Cliente: {}, FormaPago: {}",
                request.getClienteId() != null ? request.getClienteId() : request.getDocumento(),
                request.getFormaPago());

        // 1️⃣ Obtener o crear cliente
        Cliente cliente = obtenerOCrearCliente(request);

        if (request.getTipoComprobante() == TipoComprobante.FACTURA) {
            if (cliente.getDocumento().length() != 11) {
                throw new RuntimeException("⛔ Error de Facturación: No se puede emitir FACTURA a un DNI. Se requiere RUC (11 dígitos).");
            }
        }

        TipoComprobante tipo = request.getTipoComprobante();
        String serie = request.getSerie();

        if (tipo == null || serie == null || serie.trim().isEmpty()) {
            throw new RuntimeException("Debe especificar tipo y serie de comprobante");
        }

        // 3️⃣ Generar número de comprobante
        String[] comprobante = comprobanteSecuenciaService.generarNumeroComprobante(tipo, serie);
        String numero = comprobante[1];

        // 4️⃣ Crear la venta
        Venta venta = Venta.builder()
                .cliente(cliente)
                .tipoComprobante(tipo)
                .serie(serie)
                .numero(numero)
                .formaPago(request.getFormaPago())
                .descuentoGeneral(request.getDescuentoGeneral() != null ?
                        request.getDescuentoGeneral() : BigDecimal.ZERO)
                .observaciones(request.getObservaciones())
                .usuarioCreacion(usuarioCreacion)
                .estado(request.getFormaPago() == FormaPago.CONTADO ?
                    EstadoVenta.PAGADA : EstadoVenta.PENDIENTE)
                .build();

        // 5️⃣ Procesar detalles
        BigDecimal subtotalVenta = procesarDetallesVenta(venta, request.getDetalles());

        // 6️⃣ Calcular totales
        venta.setSubtotal(subtotalVenta);
        venta.calcularTotal();

        // 7️⃣ Guardar venta
        Venta ventaGuardada = ventaRepository.save(venta);
        log.info("✅ Venta creada - ID: {}, Comprobante: {}-{}, Total: {}",
                ventaGuardada.getId(), serie, numero, ventaGuardada.getTotal());

        // 8️⃣ Registrar movimientos de inventario
        Usuario usuario = usuarioRepository.findByUsuario(usuarioCreacion)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + usuarioCreacion));

        for (DetalleVenta detalle : ventaGuardada.getDetalles()) {
            registrarMovimientoInventarioPostVenta(detalle, ventaGuardada.getId(), usuario.getId());
        }

        // 9️⃣ Si es CRÉDITO, crear crédito y cuotas
        if (request.getFormaPago() == FormaPago.CREDITO && request.getCredito() != null) {
            creditoVentaService.crearCredito(ventaGuardada, request.getCredito());
            log.info("💳 Crédito generado para venta ID: {}", ventaGuardada.getId());
        }

        return convertirAVentaResponse(ventaGuardada);
    }

    

    /**
     * Registra el movimiento de inventario después de crear la venta
     */
    private void registrarMovimientoInventarioPostVenta(DetalleVenta detalle, Long ventaId, Long usuarioId) {
        try {
            RegistrarMovimientoRequest movimiento = RegistrarMovimientoRequest.builder()
                    .productoId(detalle.getProducto().getId())
                    .tipoMovimiento(TipoMovimiento.SALIDA)
                    .cantidad(detalle.getCantidad())
                    .motivo(MotivoMovimiento.VENTA)
                    .referenciaId(ventaId)
                    .referenciaTipo(TipoReferencia.VENTA)
                    .observaciones("Salida por venta #" + ventaId)
                    .build();

            movimientoInventarioService.registrarMovimiento(movimiento, usuarioId);

        } catch (Exception e) {
            log.warn("⚠️ No se pudo registrar movimiento para producto {}: {}",
                    detalle.getProducto().getId(), e.getMessage());
            // No fallar la venta si falla el registro de movimiento
        }
    }


    // ===================== CONSULTAR VENTAS =====================

    @Transactional(readOnly = true)
    public VentaResponse obtenerVentaPorId(Long id) {
        Venta venta = ventaRepository.findByIdWithDetalles(id)
                .orElseThrow(() -> new RuntimeException("Venta no encontrada con ID: " + id));
        return convertirAVentaResponse(venta);
    }

    @Transactional(readOnly = true)
    public List<VentaResponse> listarTodasLasVentas() {
        return ventaRepository.findAll().stream()
                .map(this::convertirAVentaResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<VentaResponse> listarVentasPorCliente(Long clienteId) {
        return ventaRepository.findByClienteIdOrderByFechaCreacionDesc(clienteId).stream()
                .map(this::convertirAVentaResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<VentaResponse> listarVentasPorEstado(EstadoVenta estado) {
        return ventaRepository.findByEstadoOrderByFechaCreacionDesc(estado).stream()
                .map(this::convertirAVentaResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<VentaResponse> listarVentasPorRangoFechas(LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        return ventaRepository.findByFechaCreacionBetweenOrderByFechaCreacionDesc(fechaInicio, fechaFin).stream()
                .map(this::convertirAVentaResponse)
                .collect(Collectors.toList());
    }

    /**
     * Listar ventas CON CRÉDITO (para el tab de Créditos)
     * Carga optimizada con cliente, crédito y cuotas
     */
    @Transactional(readOnly = true)
    public List<VentaResponse> listarVentasConCredito() {
        return ventaRepository.findVentasConCreditoWithDetails().stream()
                .map(this::convertirAVentaResponse)
                .collect(Collectors.toList());
    }

    // ===================== ACTUALIZAR ESTADO =====================

    @Transactional
    public VentaResponse actualizarEstadoVenta(Long id, EstadoVenta nuevoEstado) {
        Venta venta = ventaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Venta no encontrada con ID: " + id));

        venta.setEstado(nuevoEstado);
        Venta ventaActualizada = ventaRepository.save(venta);
        log.info("Estado de venta {} actualizado a: {}", id, nuevoEstado);

        return convertirAVentaResponse(ventaActualizada);
    }

    /**
     * Anula una venta y devuelve el stock
     * Si tiene crédito, lo cancela también
     */
    @Transactional
    public VentaResponse anularVenta(Long id) {
        Venta venta = ventaRepository.findByIdWithDetalles(id)
                .orElseThrow(() -> new RuntimeException("Venta no encontrada con ID: " + id));

        if (venta.getEstado() == EstadoVenta.ANULADA) {
            throw new RuntimeException("La venta ya está anulada");
        }

        // Devolver stock
        for (DetalleVenta detalle : venta.getDetalles()) {
            Product producto = detalle.getProducto();
            producto.incrementarStock(detalle.getCantidad());
            productRepository.save(producto);
        }

        venta.setEstado(EstadoVenta.ANULADA);
        Venta ventaAnulada = ventaRepository.save(venta);
        log.info("✅ Venta {} anulada exitosamente", id);

        // Si tiene crédito, cancelarlo
        if (venta.getFormaPago() == FormaPago.CREDITO) {
            creditoVentaService.cancelarCredito(venta.getId());
        }

        return convertirAVentaResponse(ventaAnulada);
    }

    // ===================== REPORTES =====================

    @Transactional(readOnly = true)
    public ReporteVentasDTO generarReporteVentas(LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        List<Venta> ventas = ventaRepository.findByFechaCreacionBetweenOrderByFechaCreacionDesc(
            fechaInicio, fechaFin
        );

        long totalVentas = ventas.stream()
                .filter(v -> v.getEstado() != EstadoVenta.ANULADA)
                .count();

        long ventasContado = ventas.stream()
                .filter(v -> v.getFormaPago() == FormaPago.CONTADO && v.getEstado() != EstadoVenta.ANULADA)
                .count();

        long ventasCredito = ventas.stream()
                .filter(v -> v.getFormaPago() == FormaPago.CREDITO && v.getEstado() != EstadoVenta.ANULADA)
                .count();

        BigDecimal montoTotalVentas = ventaRepository.sumTotalByFechaCreacionBetween(fechaInicio, fechaFin);

        BigDecimal montoVentasContado = ventas.stream()
                .filter(v -> v.getFormaPago() == FormaPago.CONTADO && v.getEstado() != EstadoVenta.ANULADA)
                .map(Venta::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal montoVentasCredito = ventas.stream()
                .filter(v -> v.getFormaPago() == FormaPago.CREDITO && v.getEstado() != EstadoVenta.ANULADA)
                .map(Venta::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long ventasAnuladas = ventas.stream()
                .filter(v -> v.getEstado() == EstadoVenta.ANULADA)
                .count();

        ReporteVentasDTO reporte = ReporteVentasDTO.builder()
                .fechaInicio(fechaInicio)
                .fechaFin(fechaFin)
                .totalVentas(totalVentas)
                .ventasContado(ventasContado)
                .ventasCredito(ventasCredito)
                .montoTotalVentas(montoTotalVentas)
                .montoVentasContado(montoVentasContado)
                .montoVentasCredito(montoVentasCredito)
                .ventasAnuladas(ventasAnuladas)
                .build();

        reporte.setPromedioVenta(reporte.calcularPromedioVenta());
        return reporte;
    }

    /**
     * Obtener estadísticas para el dashboard
     */
    @Transactional(readOnly = true)
    public Map<String, Object> obtenerEstadisticasDashboard() {
        Map<String, Object> estadisticas = new HashMap<>();

        LocalDateTime ahora = LocalDateTime.now();

        // 1. Ventas de HOY
        LocalDateTime inicioHoy = ahora.toLocalDate().atStartOfDay();
        LocalDateTime finHoy = ahora.toLocalDate().atTime(23, 59, 59);
        BigDecimal ventasHoy = ventaRepository.sumTotalByFechaCreacionBetweenAndEstadoNot(
            inicioHoy, finHoy, EstadoVenta.ANULADA
        );

        // 2. Ventas de la SEMANA (últimos 7 días)
        LocalDateTime inicioSemana = ahora.minusDays(7);
        BigDecimal ventasSemana = ventaRepository.sumTotalByFechaCreacionBetweenAndEstadoNot(
            inicioSemana, ahora, EstadoVenta.ANULADA
        );

        // 3. Ventas del MES (últimos 30 días)
        LocalDateTime inicioMes = ahora.minusDays(30);
        BigDecimal ventasMes = ventaRepository.sumTotalByFechaCreacionBetweenAndEstadoNot(
            inicioMes, ahora, EstadoVenta.ANULADA
        );

        // 4. Total de ventas (cantidad de registros activos)
        long totalVentas = ventaRepository.count() -
            ventaRepository.findByEstadoOrderByFechaCreacionDesc(EstadoVenta.ANULADA).size();

        estadisticas.put("ventasHoy", ventasHoy != null ? ventasHoy : BigDecimal.ZERO);
        estadisticas.put("ventasSemana", ventasSemana != null ? ventasSemana : BigDecimal.ZERO);
        estadisticas.put("ventasMes", ventasMes != null ? ventasMes : BigDecimal.ZERO);
        estadisticas.put("totalVentas", totalVentas);

        return estadisticas;
    }

    /**
     * Total vendido por día, para el gráfico de tendencia del dashboard.
     * Incluye días sin ventas (total 0) para que el gráfico no tenga huecos.
     */
    @Transactional(readOnly = true)
    public List<VentaPorDiaDTO> obtenerVentasPorDia(int dias) {
        LocalDateTime fin = LocalDateTime.now();
        LocalDateTime inicio = fin.toLocalDate().minusDays(dias - 1L).atStartOfDay();

        List<Venta> ventas = ventaRepository.findByFechaCreacionBetweenOrderByFechaCreacionDesc(inicio, fin)
                .stream()
                .filter(v -> v.getEstado() != EstadoVenta.ANULADA)
                .toList();

        Map<LocalDate, List<Venta>> porDia = ventas.stream()
                .collect(Collectors.groupingBy(v -> v.getFechaCreacion().toLocalDate()));

        List<VentaPorDiaDTO> resultado = new ArrayList<>();
        LocalDate cursor = inicio.toLocalDate();
        LocalDate hoy = fin.toLocalDate();
        while (!cursor.isAfter(hoy)) {
            List<Venta> delDia = porDia.getOrDefault(cursor, List.of());
            BigDecimal total = delDia.stream().map(Venta::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
            resultado.add(VentaPorDiaDTO.builder()
                    .fecha(cursor)
                    .total(total)
                    .cantidadVentas((long) delDia.size())
                    .build());
            cursor = cursor.plusDays(1);
        }
        return resultado;
    }

    /**
     * Ranking de productos más vendidos por cantidad de unidades, para el dashboard.
     */
    @Transactional(readOnly = true)
    public List<ProductoMasVendidoDTO> obtenerProductosMasVendidos(int limite) {
        return detalleVentaRepository.findProductosMasVendidos(PageRequest.of(0, limite)).stream()
                .map(row -> ProductoMasVendidoDTO.builder()
                        .productoId((Long) row[0])
                        .productoNombre((String) row[1])
                        .cantidadVendida((Long) row[2])
                        .totalIngresos((BigDecimal) row[3])
                        .build())
                .sorted(Comparator.comparing(ProductoMasVendidoDTO::getCantidadVendida).reversed())
                .toList();
    }

    // ===================== MÉTODOS PRIVADOS =====================

    /**
     * Obtiene un cliente existente o lo crea si viene por documento
     */
    private Cliente obtenerOCrearCliente(CrearVentaRequest request) {
        if (request.getClienteId() != null) {
            // Flujo tradicional: buscar por ID
            Cliente cliente = clienteRepository.findById(request.getClienteId())
                    .orElseThrow(() -> new RuntimeException("Cliente no encontrado con ID: " + request.getClienteId()));

            if (cliente.getEstado() != 1) {
                throw new RuntimeException("El cliente no está activo");
            }
            return cliente;
        } else if (request.getDocumento() != null && !request.getDocumento().trim().isEmpty()) {
            // Nuevo flujo: buscar o crear por documento
            try {
                return clienteService.obtenerOCrearCliente(request.getDocumento());
            } catch (ClienteService.ClienteException e) {
                throw new RuntimeException("Error al procesar cliente: " + e.getMessage());
            }
        } else {
            throw new RuntimeException("Debe proporcionar clienteId o documento del cliente");
        }
    }

    /**
     * Procesa los detalles de la venta: valida stock, descuenta y calcula subtotales
     */
    private BigDecimal procesarDetallesVenta(Venta venta, List<CrearVentaRequest.DetalleVentaRequest> detallesRequest) {
        BigDecimal subtotalVenta = BigDecimal.ZERO;

        for (int i = 0; i < detallesRequest.size(); i++) {
            CrearVentaRequest.DetalleVentaRequest detalleRequest = detallesRequest.get(i);

            Product producto = productRepository.findById(detalleRequest.getProductoId())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID: " + detalleRequest.getProductoId()));

            if (producto.getEstado() != 1) {
                throw new RuntimeException("El producto '" + producto.getNombre() + "' no está activo");
            }

            producto.descontarStock(detalleRequest.getCantidad());
            productRepository.save(producto);

            DetalleVenta detalle = DetalleVenta.builder()
                    .producto(producto)
                    .cantidad(detalleRequest.getCantidad())
                    .precioUnitario(detalleRequest.getPrecioUnitario())
                    .descuentoPorcentaje(detalleRequest.getDescuentoPorcentaje() != null ?
                        detalleRequest.getDescuentoPorcentaje() : BigDecimal.ZERO)
                    .build();

            detalle.calcularSubtotal();

            log.info("📦 Detalle #{} - {}", (i+1), producto.getNombre());
            log.info("   Cant: {} | Precio: {} | Desc: {}%",
                detalle.getCantidad(),
                detalle.getPrecioUnitario(),
                detalle.getDescuentoPorcentaje());
            log.info("   Subtotal detalle: {}", detalle.getSubtotal());

            subtotalVenta = subtotalVenta.add(detalle.getSubtotal());
            log.info("   Subtotal acumulado: {}", subtotalVenta);

            venta.addDetalle(detalle);
        }

        log.info("🎯 SUBTOTAL FINAL: {}", subtotalVenta);
        return subtotalVenta;
    }

    /**
     * Convierte una entidad Venta a DTO Response
     */
    private VentaResponse convertirAVentaResponse(Venta venta) {
        List<VentaResponse.DetalleVentaResponse> detallesResponse = venta.getDetalles().stream()
                .map(d -> VentaResponse.DetalleVentaResponse.builder()
                        .id(d.getId())
                        .productoId(d.getProducto().getId())
                        .productoNombre(d.getProducto().getNombre())
                        .cantidad(d.getCantidad())
                        .precioUnitario(d.getPrecioUnitario())
                        .descuentoPorcentaje(d.getDescuentoPorcentaje())
                        .subtotal(d.getSubtotal())
                        .build())
                .collect(Collectors.toList());

        return VentaResponse.builder()
                .id(venta.getId())
                .clienteId(venta.getCliente().getId())
                .clienteNombre(venta.getCliente().getNombre())
                .clienteDocumento(venta.getCliente().getDocumento())
                .tipoComprobante(venta.getTipoComprobante())
                .serie(venta.getSerie())
                .numero(venta.getNumero())
                .comprobanteCompleto(venta.getComprobanteCompleto())
                .formaPago(venta.getFormaPago())
                .subtotal(venta.getSubtotal())
                .descuentoGeneral(venta.getDescuentoGeneral())
                .igv(venta.getIgv())
                .total(venta.getTotal())
                .estado(venta.getEstado())
                .observaciones(venta.getObservaciones())
                .usuarioCreacion(venta.getUsuarioCreacion())
                .fechaCreacion(venta.getFechaCreacion())
                .fechaActualizacion(venta.getFechaActualizacion())
                .detalles(detallesResponse)
                .tieneCredito(venta.esCredito())
                .build();
    }
}


