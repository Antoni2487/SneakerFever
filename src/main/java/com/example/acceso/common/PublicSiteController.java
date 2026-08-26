package com.example.acceso.common;

import com.example.acceso.brand.BrandResponse;
import com.example.acceso.brand.BrandService;
import com.example.acceso.product.ProductResponse;
import com.example.acceso.product.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    private final BrandService brandService;
    private final ProductService productService;

    public PublicSiteController(BrandService brandService, ProductService productService) {
        this.brandService = brandService;
        this.productService = productService;
    }

    @GetMapping("/marcas")
    public ResponseEntity<Map<String, Object>> listarMarcas() {
        List<BrandResponse> marcas = brandService.listarMarcasConImagen().stream()
                .map(BrandResponse::from)
                .toList();
        return ResponseEntity.ok(Map.of("success", true, "data", marcas));
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

    private List<ProductResponse> mapear(List<com.example.acceso.product.Product> productos) {
        return productos.stream().map(ProductResponse::from).toList();
    }
}
