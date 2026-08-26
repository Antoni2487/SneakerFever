package com.example.acceso.common;

import com.example.acceso.brand.BrandResponse;
import com.example.acceso.brand.BrandService;
import com.example.acceso.category.CategoryResponse;
import com.example.acceso.category.CategoryService;
import com.example.acceso.product.ProductResponse;
import com.example.acceso.product.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Endpoints públicos (sin sesión) para el sitio público en React: landing, catálogo, etc.
 * Deliberadamente separado de los controllers de dominio (Brand/Product), que son admin-only,
 * para no mezclar la superficie pública con la de gestión.
 */
@RestController
@RequestMapping("/api/public")
public class PublicSiteController {

    private static final Logger log = LoggerFactory.getLogger(PublicSiteController.class);

    private final BrandService brandService;
    private final ProductService productService;
    private final CategoryService categoryService;
    private final RestTemplate restTemplate;

    public PublicSiteController(BrandService brandService, ProductService productService,
                                 CategoryService categoryService, RestTemplate restTemplate) {
        this.brandService = brandService;
        this.productService = productService;
        this.categoryService = categoryService;
        this.restTemplate = restTemplate;
    }

    @GetMapping("/marcas")
    public ResponseEntity<Map<String, Object>> listarMarcas() {
        List<BrandResponse> marcas = brandService.listarMarcasConImagen().stream()
                .map(BrandResponse::from)
                .toList();
        return ResponseEntity.ok(Map.of("success", true, "data", marcas));
    }

    @GetMapping("/categorias")
    public ResponseEntity<Map<String, Object>> listarCategorias() {
        List<CategoryResponse> categorias = categoryService.listarCategorias().stream()
                .map(CategoryResponse::from)
                .toList();
        return ResponseEntity.ok(Map.of("success", true, "data", categorias));
    }

    @GetMapping("/productos")
    public ResponseEntity<Map<String, Object>> listarProductos() {
        return ResponseEntity.ok(Map.of("success", true, "data", mapear(productService.listarProductos())));
    }

    @GetMapping("/productos/{id}")
    public ResponseEntity<Map<String, Object>> obtenerProducto(@PathVariable Long id) {
        return productService.obtenerProductoPorId(id)
                .filter(p -> p.getEstado() == 1)
                .map(p -> ResponseEntity.ok(Map.<String, Object>of("success", true, "data", ProductResponse.from(p))))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("success", false, "message", "Producto no encontrado")));
    }

    @GetMapping("/productos/destacados")
    public ResponseEntity<Map<String, Object>> productosDestacados() {
        Map<String, Object> destacados = new HashMap<>();
        destacados.put("zapatillas", mapear(productService.obtenerDestacadosPorCategoria("Zapatillas")));
        destacados.put("ropa", mapear(productService.obtenerDestacadosPorCategoria("Ropa")));
        destacados.put("accesorios", mapear(productService.obtenerDestacadosPorCategoria("Accesorios")));
        return ResponseEntity.ok(Map.of("success", true, "data", destacados));
    }

    @GetMapping("/productos/mas-vendidos")
    public ResponseEntity<Map<String, Object>> productosMasVendidos() {
        return ResponseEntity.ok(Map.of("success", true, "data", mapear(productService.obtenerZapatillasMasVendidas())));
    }

    @PostMapping("/contacto")
    public ResponseEntity<Map<String, Object>> enviarContacto(@RequestBody ContactoRequest request) {
        try {
            String urlNode = "http://127.0.0.1:3000/api/contacto";
            Map<String, String> body = new HashMap<>();
            body.put("nombre", request.nombre());
            body.put("correo", request.correo());
            body.put("asunto", request.asunto());
            body.put("mensaje", request.mensaje());
            restTemplate.postForObject(urlNode, body, String.class);
            return ResponseEntity.ok(Map.of("success", true, "message", "¡Mensaje enviado! Se guardó en nuestro sistema externo."));
        } catch (Exception e) {
            log.error("Error enviando contacto a Node: {}", e.getMessage());
            return ResponseEntity.ok(Map.of("success", false,
                    "message", "Hubo un problema de conexión, pero intentaremos procesar tu mensaje."));
        }
    }

    private List<ProductResponse> mapear(List<com.example.acceso.product.Product> productos) {
        return productos.stream().map(ProductResponse::from).toList();
    }

    public record ContactoRequest(String nombre, String correo, String asunto, String mensaje) {}
}
