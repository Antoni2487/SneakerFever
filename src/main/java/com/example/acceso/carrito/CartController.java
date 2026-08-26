package com.example.acceso.carrito;

import com.example.acceso.product.Product;
import com.example.acceso.usuario.Usuario;
import com.example.acceso.product.ProductService;
import com.example.acceso.venta.CrearVentaRequest;
import com.example.acceso.venta.CrearVentaRequest.DetalleVentaRequest;
import com.example.acceso.venta.FormaPago;
import com.example.acceso.venta.TipoComprobante;
import com.example.acceso.venta.VentaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/carrito")
public class CartController {

    private final ProductService productService;
    private final VentaService ventaService;

    public CartController(ProductService productService, VentaService ventaService) {
        this.productService = productService;
        this.ventaService = ventaService;
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
        try {
            Carrito carrito = obtenerCarritoSession(session);
            Product producto = productService.obtenerProductoPorId(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado")); 

            if (producto.getStock() < cantidad) {
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
            // Validar stock aquí si quieres ser muy estricto, 
            // por ahora confiamos en la actualización
            carrito.actualizarCantidad(id, cantidad);
        }
        
        return ResponseEntity.ok(Map.of("success", true));
    }

    // 5. Checkout desde el sitio público en JSON (variante de CheckoutController.procesarCompra,
    // que devuelve redirect+flash pensado para el form Thymeleaf). Igual que el original, exige
    // sesión de Usuario (no hay cuenta de cliente separada en este backend) - ver CheckoutController.
    @PostMapping("/api/checkout")
    @ResponseBody
    public ResponseEntity<?> checkout(@RequestBody CheckoutRequest request, HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
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
            ventaRequest.setTipoComprobante(TipoComprobante.valueOf(request.tipoComprobante()));
            ventaRequest.setSerie(request.tipoComprobante().equals("FACTURA") ? "F001" : "B001");
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
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Error al procesar la compra: " + e.getMessage()));
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