package com.example.acceso.carrito;

import com.example.acceso.common.ApiResponses;
import com.example.acceso.producto.Producto;
import com.example.acceso.usuario.Usuario;
import com.example.acceso.producto.ProductoService;
import com.example.acceso.venta.CrearVentaRequest;
import com.example.acceso.venta.CrearVentaRequest.DetalleVentaRequest;
import com.example.acceso.venta.FormaPago;
import com.example.acceso.venta.TipoComprobante;
import com.example.acceso.venta.VentaService;
import com.example.acceso.venta.ComprobanteSecuenciaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/carrito")
public class CartController {

    private final ProductoService productService;
    private final VentaService ventaService;
    private final ComprobanteSecuenciaService comprobanteSecuenciaService;

    public CartController(ProductoService productService, VentaService ventaService,
            ComprobanteSecuenciaService comprobanteSecuenciaService) {
        this.productService = productService;
        this.ventaService = ventaService;
        this.comprobanteSecuenciaService = comprobanteSecuenciaService;
    }

    // 1. Obtener el carrito actual (JSON para el sidebar)
    @GetMapping("/api/ver")
    @ResponseBody
    public Carrito verCarrito(HttpSession session) {
        return obtenerCarritoSession(session);
    }

    // 2. Agregar producto al carrito
    @PostMapping("/api/agregar/{id}")
    @ResponseBody
    public ResponseEntity<?> agregarProducto(@PathVariable Long id, @RequestParam(defaultValue = "1") Integer cantidad, HttpSession session) {
        if (cantidad == null || cantidad < 1) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "La cantidad debe ser al menos 1"));
        }
        try {
            Carrito carrito = obtenerCarritoSession(session);
            Producto producto = productService.obtenerProductoPorId(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

            // Contra lo que ya hay en el carrito, no solo contra lo que se agrega ahora:
            // agregarItem suma la cantidad si el producto ya estaba.
            if (producto.getStock() < cantidadEnCarrito(carrito, id) + cantidad) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Stock insuficiente"));
            }

            ItemCarrito item = new ItemCarrito();
            item.setProductoId(producto.getId());
            item.setNombre(producto.getNombre());
            item.setPrecio(producto.getPrecio()); // Ojo: Aplicar descuento si tienes lógica de oferta
            item.setImagen(producto.getImagen());
            item.setCantidad(cantidad);
            item.calcularSubtotal();

            carrito.agregarItem(item);
            
            return ResponseEntity.ok(Map.of("success", true, "message", "Producto agregado", "totalItems", carrito.getItems().size()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Error al agregar"));
        }
    }

    // 3. Eliminar item
    @DeleteMapping("/api/eliminar/{id}")
    @ResponseBody
    public ResponseEntity<?> eliminarItem(@PathVariable Long id, HttpSession session) {
        Carrito carrito = obtenerCarritoSession(session);
        carrito.eliminarItem(id);
        return ResponseEntity.ok(Map.of("success", true));
    }
    

    @PutMapping("/api/actualizar/{id}")
    @ResponseBody
    public ResponseEntity<?> actualizarCantidad(@PathVariable Long id, 
                                              @RequestParam Integer cantidad, 
                                              HttpSession session) {
        Carrito carrito = obtenerCarritoSession(session);

        if (cantidad <= 0) {
            carrito.eliminarItem(id); // Si baja a 0, se elimina
        } else {
            Producto producto = productService.obtenerProductoPorId(id).orElse(null);
            if (producto == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Producto no encontrado"));
            }
            if (producto.getStock() < cantidad) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Stock insuficiente"));
            }
            carrito.actualizarCantidad(id, cantidad);
        }

        return ResponseEntity.ok(Map.of("success", true));
    }

    private int cantidadEnCarrito(Carrito carrito, Long productoId) {
        return carrito.getItems().stream()
                .filter(item -> item.getProductoId().equals(productoId))
                .mapToInt(ItemCarrito::getCantidad)
                .sum();
    }

    // 5. Checkout desde el sitio público en JSON (variante de CheckoutController.procesarCompra,
    // que devuelve redirect+flash pensado para el form Thymeleaf). Exige un Usuario autenticado
    // (no hay cuenta de cliente separada en este backend), pero por JWT: el login de React nunca
    // escribe "usuarioLogueado" en HttpSession, así que leerlo de ahí daba 401 siempre.
    // SecurityConfig ya rechaza esta ruta sin token; el null-check queda como red de seguridad.
    @PostMapping("/api/checkout")
    @ResponseBody
    public ResponseEntity<?> checkout(@RequestBody CheckoutRequest request,
            @AuthenticationPrincipal Usuario usuario, HttpSession session) {
        if (usuario == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Debes iniciar sesión para completar la compra"));
        }

        Carrito carrito = obtenerCarritoSession(session);
        if (carrito.getItems().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "El carrito está vacío"));
        }

        try {
            CrearVentaRequest ventaRequest = new CrearVentaRequest();
            ventaRequest.setDocumento(request.documento());
            TipoComprobante tipoComprobante = TipoComprobante.valueOf(request.tipoComprobante());
            ventaRequest.setTipoComprobante(tipoComprobante);
            // Serie resuelta desde la misma fuente de verdad que usa el POS admin
            // (ComprobanteSecuenciaService), no hardcodeada - ver VentaController.obtenerSerieActiva().
            String serie = comprobanteSecuenciaService.obtenerSerieActiva(tipoComprobante)
                    .orElseThrow(() -> new RuntimeException(
                            "No hay una serie activa configurada para " + tipoComprobante))
                    .getSerie();
            ventaRequest.setSerie(serie);
            ventaRequest.setFormaPago(FormaPago.CONTADO);
            ventaRequest.setObservaciones("Compra Web - Usuario: " + usuario.getUsuario());

            List<DetalleVentaRequest> detalles = new ArrayList<>();
            for (ItemCarrito item : carrito.getItems()) {
                DetalleVentaRequest detalle = new DetalleVentaRequest();
                detalle.setProductoId(item.getProductoId());
                detalle.setCantidad(item.getCantidad());
                detalle.setPrecioUnitario(item.getPrecio());
                detalle.setDescuentoPorcentaje(BigDecimal.ZERO);
                detalles.add(detalle);
            }
            ventaRequest.setDetalles(detalles);

            ventaService.crearVenta(ventaRequest, usuario.getUsuario());
            session.removeAttribute("carrito");

            return ResponseEntity.ok(Map.of("success", true, "message", "¡Compra realizada con éxito! Tu pedido está siendo procesado."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Error al procesar la compra: " + ApiResponses.mensajeSeguro(e)));
        }
    }

    public record CheckoutRequest(String documento, String tipoComprobante) {}

    // Método auxiliar para no repetir código
    private Carrito obtenerCarritoSession(HttpSession session) {
        Carrito carrito = (Carrito) session.getAttribute("carrito");
        if (carrito == null) {
            carrito = new Carrito();
            session.setAttribute("carrito", carrito);
        }
        return carrito;
    }
}