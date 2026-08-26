package com.example.acceso.product;

import com.example.acceso.brand.BrandService;
import com.example.acceso.category.CategoryService;
import com.example.acceso.common.ApiResponses;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/productos")
public class ProductController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final BrandService brandService;

    public ProductController(ProductService productService,
                            CategoryService categoryService,
                            BrandService brandService) {
        this.productService = productService;
        this.categoryService = categoryService;
        this.brandService = brandService;
    }

    // ===================== Vistas =====================

    /**
     * Página principal de gestión de productos
     */
    @GetMapping({"", "/"})
    public String listarProductos(Model model) {
        model.addAttribute("title", "Gestión de Productos");
        model.addAttribute("totalProductos", productService.contarProductos());
        model.addAttribute("totalTodosProductos", productService.contarTodosProductos());
        model.addAttribute("categorias", categoryService.listarCategorias());
        model.addAttribute("marcas", brandService.listarMarcas());
        model.addAttribute("generos", com.example.acceso.product.Genero.values());
        return "admin/productos";
    }

    @GetMapping("/listar")
    public String listarProductosRedirect() {
        return "redirect:/productos";
    }

    // ===================== API REST - Listados =====================

    /**
     * Obtener todos los productos activos (JSON)
     */
    @GetMapping("/api/listar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> listarProductosJson(
            @RequestParam(required = false, defaultValue = "false") Boolean ordenado) {
        try {
            List<ProductResponse> productos = (ordenado
                ? productService.listarProductosOrdenados()
                : productService.listarProductos()).stream().map(ProductResponse::from).toList();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", productos);
            response.put("total", productos.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ApiResponses.error("Error al listar productos: " + e.getMessage());
        }
    }

    /**
     * Obtener todos los productos (activos e inactivos) para DataTables
     */
    @GetMapping("/api/datatables")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> listarParaDataTables() {
        try {
            List<ProductResponse> productos = productService.listarTodosProductos().stream()
                    .map(ProductResponse::from)
                    .toList();

            Map<String, Object> response = new HashMap<>();
            response.put("draw", 1);
            response.put("recordsTotal", productos.size());
            response.put("recordsFiltered", productos.size());
            response.put("data", productos);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ApiResponses.error("Error al cargar datos para la tabla: " + e.getMessage());
        }
    }
    /**
     * Obtener estadísticas de productos (activos, inactivos, eliminados)
     */
    @GetMapping("/api/estadisticas")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> obtenerEstadisticas() {
        try {
            List<Product> todosProductos = productService.listarTodosProductos();

            // Contar por estado
            long activos = todosProductos.stream()
                .filter(p -> p.getEstado() == 1)
                .count();

            long inactivos = todosProductos.stream()
                .filter(p -> p.getEstado() == 0)
                .count();

            long eliminados = todosProductos.stream()
                .filter(p -> p.getEstado() == 2)
                .count();

            // Preparar datos de estadísticas
            Map<String, Object> stats = new HashMap<>();
            stats.put("activos", activos);
            stats.put("inactivos", inactivos);
            stats.put("eliminados", eliminados);
            stats.put("total", todosProductos.size());

            // Respuesta exitosa
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", stats);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ApiResponses.error("Error al obtener estadísticas: " + e.getMessage());
        }
    }
    /**
     * Obtener productos disponibles para ventas (solo datos necesarios)
     */
    @GetMapping("/api/listarDisponibles")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> listarProductosDisponibles() {
        try {
            List<Product> productos = productService.listarProductos().stream()
                    .filter(p -> p.getStock() > 0 && p.getEstado() == 1)
                    .collect(Collectors.toList());

            // Mapear solo los campos necesarios para evitar problemas de lazy loading
            List<Map<String, Object>> productosSimplificados = productos.stream()
                    .map(p -> {
                        Map<String, Object> prod = new HashMap<>();
                        prod.put("id", p.getId());
                        prod.put("nombre", p.getNombre());
                        prod.put("precio", p.getPrecio());
                        prod.put("stock", p.getStock());
                        prod.put("imagen", p.getImagen()); // Imagen principal
                        prod.put("descuento", p.getDescuento()); // ✅ AGREGAR ESTA LÍNEA

                        // Código del producto (genera uno si no existe)
                        String codigo = "PROD-" + String.format("%05d", p.getId());
                        prod.put("codigo", codigo);

                        return prod;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(productosSimplificados);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ArrayList<>());
        }
    }

    // ===================== API REST - Filtros Navbar =====================

    /**
     * Obtener productos destacados
     */
    @GetMapping("/api/destacados")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> productosDestacados(
            @RequestParam(required = false) String genero) {
        try {
            List<Product> productosEntidad;

            if (genero != null && !genero.isEmpty()) {
                Genero generoEnum = Genero.valueOf(genero.toUpperCase());
                productosEntidad = productService.listarDestacadosPorGenero(generoEnum);
            } else {
                productosEntidad = productService.listarDestacados();
            }
            List<ProductResponse> productos = productosEntidad.stream().map(ProductResponse::from).toList();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", productos);
            response.put("total", productos.size());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ApiResponses.error("Género no válido");
        } catch (Exception e) {
            return ApiResponses.error("Error al obtener destacados: " + e.getMessage());
        }
    }

    /**
     * Obtener productos por género
     */
    @GetMapping("/api/genero/{genero}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> productosPorGenero(
            @PathVariable String genero,
            @RequestParam(required = false, defaultValue = "false") Boolean todos) {
        try {
            Genero generoEnum = Genero.valueOf(genero.toUpperCase());
            List<ProductResponse> productos = (todos
                ? productService.listarTodosPorGenero(generoEnum)
                : productService.listarPorGenero(generoEnum)).stream().map(ProductResponse::from).toList();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", productos);
            response.put("total", productos.size());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ApiResponses.error("Género no válido");
        } catch (Exception e) {
            return ApiResponses.error("Error al filtrar por género: " + e.getMessage());
        }
    }

    /**
     * Obtener productos en rebaja (SALE)
     */
    @GetMapping("/api/sale")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> productosEnRebaja(
            @RequestParam(required = false) String genero) {
        try {
            List<Product> productosEntidad;

            if (genero != null && !genero.isEmpty()) {
                Genero generoEnum = Genero.valueOf(genero.toUpperCase());
                productosEntidad = productService.listarEnRebajaPorGenero(generoEnum);
            } else {
                productosEntidad = productService.listarEnRebaja();
            }
            List<ProductResponse> productos = productosEntidad.stream().map(ProductResponse::from).toList();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", productos);
            response.put("total", productos.size());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ApiResponses.error("Género no válido");
        } catch (Exception e) {
            return ApiResponses.error("Error al obtener productos en rebaja: " + e.getMessage());
        }
    }

    // ===================== API REST - Por Categoría/Marca =====================

    /**
     * Obtener productos por categoría
     */
    @GetMapping("/api/categoria/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> productosPorCategoria(@PathVariable Long id) {
        try {
            List<ProductResponse> productos = productService.listarPorCategoria(id).stream()
                    .map(ProductResponse::from)
                    .toList();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", productos);
            response.put("total", productos.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ApiResponses.error("Error al filtrar por categoría: " + e.getMessage());
        }
    }

    /**
     * Obtener productos por marca
     */
    @GetMapping("/api/marca/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> productosPorMarca(@PathVariable Long id) {
        try {
            List<ProductResponse> productos = productService.listarPorMarca(id).stream()
                    .map(ProductResponse::from)
                    .toList();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", productos);
            response.put("total", productos.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ApiResponses.error("Error al filtrar por marca: " + e.getMessage());
        }
    }

    // ===================== API REST - CRUD =====================

    /**
     * Obtener producto por ID
     */
    @GetMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> obtenerProducto(@PathVariable Long id) {
        try {
            Optional<Product> producto = productService.obtenerProductoPorId(id);

            Map<String, Object> response = new HashMap<>();
            if (producto.isPresent()) {
                response.put("success", true);
                response.put("data", ProductResponse.from(producto.get()));
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Producto no encontrado");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (Exception e) {
            return ApiResponses.error("Error al obtener el producto: " + e.getMessage());
        }
    }

    /**
     * Crear nuevo producto
     */
    @PostMapping("/api/crear")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> crearProducto(@Valid @RequestBody ProductRequest request, BindingResult result) {
        if (result.hasErrors()) {
            return ApiResponses.validationError(result);
        }

        try {
            if (productService.existeProducto(request.getNombre())) {
                return ApiResponses.error("Ya existe un producto con ese nombre");
            }

            Product nuevoProducto = productService.guardarProducto(request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Producto creado exitosamente");
            response.put("data", ProductResponse.from(nuevoProducto));

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (ProductService.ProductoException e) {
            return ApiResponses.error(e.getMessage());
        } catch (Exception e) {
            return ApiResponses.error("Error al crear el producto: " + e.getMessage());
        }
    }

    /**
     * Actualizar producto existente
     */
    @PutMapping("/api/actualizar/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> actualizarProducto(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request,
            BindingResult result) {

        if (result.hasErrors()) {
            return ApiResponses.validationError(result);
        }

        try {
            if (productService.obtenerProductoPorId(id).isEmpty()) {
                return ApiResponses.error("Producto no encontrado");
            }

            if (productService.existeProductoParaActualizar(request.getNombre(), id)) {
                return ApiResponses.error("Ya existe otro producto con ese nombre");
            }

            request.setId(id);
            Product productoActualizado = productService.guardarProducto(request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Producto actualizado exitosamente");
            response.put("data", ProductResponse.from(productoActualizado));

            return ResponseEntity.ok(response);

        } catch (ProductService.ProductoException e) {
            return ApiResponses.error(e.getMessage());
        } catch (Exception e) {
            return ApiResponses.error("Error al actualizar el producto: " + e.getMessage());
        }
    }

    /**
     * Eliminar producto (eliminación lógica)
     */
    @DeleteMapping("/api/eliminar/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> eliminarProducto(@PathVariable Long id) {
        try {
            productService.eliminarProducto(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Producto eliminado exitosamente");

            return ResponseEntity.ok(response);

        } catch (ProductService.ProductoException e) {
            return ApiResponses.error(e.getMessage());
        } catch (Exception e) {
            return ApiResponses.error("Error al eliminar el producto: " + e.getMessage());
        }
    }

    /**
     * Cambiar estado de producto (activar/desactivar)
     */
    @PutMapping("/api/cambiar-estado/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> cambiarEstado(@PathVariable Long id) {
        try {
            Optional<Product> producto = productService.cambiarEstadoProducto(id);

            Map<String, Object> response = new HashMap<>();
            if (producto.isPresent()) {
                response.put("success", true);
                response.put("message", "Estado cambiado exitosamente");
                response.put("data", ProductResponse.from(producto.get()));
                return ResponseEntity.ok(response);
            } else {
                return ApiResponses.error("Producto no encontrado");
            }

        } catch (Exception e) {
            return ApiResponses.error("Error al cambiar el estado: " + e.getMessage());
        }
    }

    /**
     * Cambiar estado destacado de producto
     */
    @PutMapping("/api/destacado/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> cambiarDestacado(@PathVariable Long id) {
        try {
            Optional<Product> producto = productService.cambiarEstadoDestacado(id);

            Map<String, Object> response = new HashMap<>();
            if (producto.isPresent()) {
                response.put("success", true);
                response.put("message", "Estado destacado cambiado exitosamente");
                response.put("data", ProductResponse.from(producto.get()));
                return ResponseEntity.ok(response);
            } else {
                return ApiResponses.error("Producto no encontrado");
            }

        } catch (Exception e) {
            return ApiResponses.error("Error al cambiar el destacado: " + e.getMessage());
        }
    }

    // ===================== API REST - Búsquedas =====================

    /**
     * Buscar productos por texto
     */
    @GetMapping("/api/buscar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> buscarProductos(
            @RequestParam String q,
            @RequestParam(required = false, defaultValue = "false") Boolean todos) {
        try {
            List<ProductResponse> productos = (todos
                ? productService.buscarProductosTodos(q)
                : productService.buscarProductos(q)).stream().map(ProductResponse::from).toList();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", productos);
            response.put("total", productos.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ApiResponses.error("Error en la búsqueda: " + e.getMessage());
        }
    }

    /**
     * Buscar por rango de precio
     */
    @GetMapping("/api/rango-precio")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> buscarPorRangoPrecio(
            @RequestParam BigDecimal min,
            @RequestParam BigDecimal max) {
        try {
            List<ProductResponse> productos = productService.buscarPorRangoPrecio(min, max).stream()
                    .map(ProductResponse::from)
                    .toList();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", productos);
            response.put("total", productos.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ApiResponses.error("Error al buscar por precio: " + e.getMessage());
        }
    }

    /**
     * Obtener productos recientes
     */
    @GetMapping("/api/recientes")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> productosRecientes() {
        try {
            List<ProductResponse> productos = productService.obtenerProductosRecientes().stream()
                    .map(ProductResponse::from)
                    .toList();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", productos);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ApiResponses.error("Error al obtener recientes: " + e.getMessage());
        }
    }

    /**
     * Obtener destacados recientes
     */
    @GetMapping("/api/destacados-recientes")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> destacadosRecientes() {
        try {
            List<ProductResponse> productos = productService.obtenerDestacadosRecientes().stream()
                    .map(ProductResponse::from)
                    .toList();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", productos);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ApiResponses.error("Error al obtener destacados recientes: " + e.getMessage());
        }
    }

    // ===================== API REST - Gestión de Stock =====================

    /**
     * Obtener productos con stock bajo
     */
    @GetMapping("/api/stock-bajo")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> productosStockBajo(
            @RequestParam(required = false, defaultValue = "10") Integer limite) {
        try {
            List<ProductResponse> productos = productService.listarStockBajo(limite).stream()
                    .map(ProductResponse::from)
                    .toList();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", productos);
            response.put("total", productos.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ApiResponses.error("Error al obtener productos con stock bajo: " + e.getMessage());
        }
    }
    /**
     * Obtener productos con stock disponible
     */
    @GetMapping("/api/stock-disponible")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> productosStockDisponible(
            @RequestParam(required = false, defaultValue = "0") Integer minimo) {
        try {
            List<ProductResponse> productos = productService.listarConStockDisponible(minimo).stream()
                    .map(ProductResponse::from)
                    .toList();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", productos);
            response.put("total", productos.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ApiResponses.error("Error al obtener productos con stock: " + e.getMessage());
        }
    }

    /**
     * Actualizar stock de un producto
     */
    @PutMapping("/api/stock/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> actualizarStock(
            @PathVariable Long id,
            @RequestParam Integer stock) {
        try {
            Optional<Product> producto = productService.actualizarStock(id, stock);

            Map<String, Object> response = new HashMap<>();
            if (producto.isPresent()) {
                response.put("success", true);
                response.put("message", "Stock actualizado exitosamente");
                response.put("data", ProductResponse.from(producto.get()));
                return ResponseEntity.ok(response);
            } else {
                return ApiResponses.error("No se pudo actualizar el stock");
            }
        } catch (Exception e) {
            return ApiResponses.error("Error al actualizar stock: " + e.getMessage());
        }
    }

}