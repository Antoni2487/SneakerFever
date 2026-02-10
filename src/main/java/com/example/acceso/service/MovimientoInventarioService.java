package com.example.acceso.service;

import com.example.acceso.dto.MovimientoInventarioResponse;
import com.example.acceso.dto.RegistrarMovimientoRequest;
import com.example.acceso.model.MovimientoInventario;
import com.example.acceso.model.Product;
import com.example.acceso.model.Usuario;
import com.example.acceso.model.EnumInventario.TipoMovimiento;
import com.example.acceso.repository.MovimientoInventarioRepository;
import com.example.acceso.repository.ProductRepository;
import com.example.acceso.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MovimientoInventarioService {

    private final MovimientoInventarioRepository movimientoRepository;
    private final ProductRepository productRepository;
    private final UsuarioRepository usuarioRepository;

    /**
     * Registra un movimiento de inventario y actualiza el stock del producto
     */
    @Transactional
    public MovimientoInventarioResponse registrarMovimiento(RegistrarMovimientoRequest request, Long usuarioId) {
        log.info("📦 Registrando movimiento de inventario - Producto: {}, Tipo: {}, Cantidad: {}",
                request.getProductoId(), request.getTipoMovimiento(), request.getCantidad());

        // 1. Validar producto
        Product producto = productRepository.findById(request.getProductoId())
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID: " + request.getProductoId()));

        // 2. Validar usuario
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + usuarioId));

        // 3. Capturar stock anterior
        Integer stockAnterior = producto.getStock();

        // 4. Calcular nuevo stock según tipo de movimiento
        Integer nuevoStock = calcularNuevoStock(producto.getStock(), request.getCantidad(), request.getTipoMovimiento());

        // 5. Validar stock suficiente para salidas/mermas
        if (nuevoStock < 0) {
            throw new RuntimeException(String.format(
                    "Stock insuficiente. Stock actual: %d, Cantidad solicitada: %d",
                    producto.getStock(), request.getCantidad()
            ));
        }

        // 6. Actualizar stock del producto
        producto.setStock(nuevoStock);
        productRepository.save(producto);

        // 7. Crear registro de movimiento
        MovimientoInventario movimiento = MovimientoInventario.builder()
                .producto(producto)
                .tipoMovimiento(request.getTipoMovimiento())
                .cantidad(request.getCantidad())
                .stockAnterior(stockAnterior)
                .stockNuevo(nuevoStock)
                .motivo(request.getMotivo())
                .referenciaId(request.getReferenciaId())
                .referenciaTipo(request.getReferenciaTipo())
                .observaciones(request.getObservaciones())
                .usuario(usuario)
                .build();

        MovimientoInventario movimientoGuardado = movimientoRepository.save(movimiento);

        log.info("✅ Movimiento registrado - ID: {}, Stock: {} → {}",
                movimientoGuardado.getId(), stockAnterior, nuevoStock);

        return convertirAResponse(movimientoGuardado);
    }

    /**
     * Calcula el nuevo stock según el tipo de movimiento
     */
    private Integer calcularNuevoStock(Integer stockActual, Integer cantidad, TipoMovimiento tipo) {
        return switch (tipo) {
            case ENTRADA, DEVOLUCION -> stockActual + cantidad;
            case SALIDA, MERMA -> stockActual - cantidad;
        };
    }

    /**
     * Listar movimientos por producto
     */
    @Transactional(readOnly = true)
    public List<MovimientoInventarioResponse> listarPorProducto(Long productoId) {
        return movimientoRepository.findByProductoIdOrderByFechaMovimientoDesc(productoId).stream()
                .map(this::convertirAResponse)
                .collect(Collectors.toList());
    }

    /**
     * Listar movimientos por tipo
     */
    @Transactional(readOnly = true)
    public List<MovimientoInventarioResponse> listarPorTipo(TipoMovimiento tipo) {
        return movimientoRepository.findByTipoMovimientoOrderByFechaMovimientoDesc(tipo).stream()
                .map(this::convertirAResponse)
                .collect(Collectors.toList());
    }

    /**
     * Listar movimientos por rango de fechas
     */
    @Transactional(readOnly = true)
    public List<MovimientoInventarioResponse> listarPorFechas(LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        return movimientoRepository.findByFechaMovimientoBetween(fechaInicio, fechaFin).stream()
                .map(this::convertirAResponse)
                .collect(Collectors.toList());
    }

    /**
     * Últimos movimientos
     */
    @Transactional(readOnly = true)
    public List<MovimientoInventarioResponse> listarUltimos() {
        return movimientoRepository.findTop10ByOrderByFechaMovimientoDesc().stream()
                .map(this::convertirAResponse)
                .collect(Collectors.toList());
    }

    /**
     * Obtener kardex de un producto
     */
    @Transactional(readOnly = true)
    public List<MovimientoInventarioResponse> obtenerKardex(Long productoId) {
        return listarPorProducto(productoId);
    }

    /**
     * Convertir entidad a DTO
     */
    private MovimientoInventarioResponse convertirAResponse(MovimientoInventario movimiento) {
        return MovimientoInventarioResponse.builder()
                .id(movimiento.getId())
                .productoId(movimiento.getProducto().getId())
                .productoNombre(movimiento.getProducto().getNombre())
                .tipoMovimiento(movimiento.getTipoMovimiento())
                .cantidad(movimiento.getCantidad())
                .stockAnterior(movimiento.getStockAnterior())
                .stockNuevo(movimiento.getStockNuevo())
                .motivo(movimiento.getMotivo())
                .referenciaId(movimiento.getReferenciaId())
                .referenciaTipo(movimiento.getReferenciaTipo())
                .observaciones(movimiento.getObservaciones())
                .usuarioId(movimiento.getUsuario().getId())
                .usuarioNombre(movimiento.getUsuario().getUsuario())
                .fechaMovimiento(movimiento.getFechaMovimiento())
                .build();
    }
}