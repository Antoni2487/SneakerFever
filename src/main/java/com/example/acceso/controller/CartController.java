package com.example.acceso.controller;

import com.example.acceso.dto.Carrito;
import com.example.acceso.dto.ItemCarrito;
import com.example.acceso.model.Product;
import com.example.acceso.model.Usuario;
import com.example.acceso.service.ProductService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping("/carrito")
public class CartController {

    private final ProductService productService;

    public CartController(ProductService productService) {
        this.productService = productService;
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