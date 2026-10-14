package com.example.acceso.producto;

import org.springframework.data.domain.Sort;
import com.example.acceso.common.Paginacion;
import org.springframework.security.access.prepost.PreAuthorize;
import com.example.acceso.seguridad.Permisos;
import com.example.acceso.marca.MarcaService;
import com.example.acceso.categoria.CategoriaService;
import com.example.acceso.common.ApiResponses;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/productos")
@PreAuthorize("hasAuthority('" + Permisos.PRODUCTOS + "')")
public class ProductoController {

    private final ProductoService productService;
    private final CategoriaService categoryService;
    private final MarcaService brandService;

    public ProductoController(ProductoService productService,
                            CategoriaService categoryService,
                            MarcaService brandService) {
        this.productService = productService;
        this.categoryService = categoryService;
        this.brandService = brandService;
    }


    // ===================== API REST - Listados =====================

    /**
     * Obtener todos los productos activos (JSON)
     */
    @GetMapping("/api/listar")
    public ResponseEntity<Map<String, Object>> listarProductosJson(
            @RequestParam(required = false, defaultValue = "false") Boolean ordenado) {
        try {
            List<ProductoResponse> productos = (ordenado
                ? productService.listarProductosOrdenados()
                : productService.listarProductos()).stream().map(ProductoResponse::from).toList();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", productos);
            response.put("total", productos.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ApiResponses.error("Error al listar productos: " + ApiResponses.mensajeSeguro(e));
        }
    }

    /**
     * Obtener todos los productos (activos e inactivos) para DataTables
     */
    /**
     * Listado paginado del panel: {@code ?page=0&size=10&q=texto}. Busca por nombre; excluye eliminados; más recientes primero.
     */
    @GetMapping("/api/pagina")
    public ResponseEntity<Map<String, Object>> paginarProductos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + Paginacion.TAMANO_POR_DEFECTO) int size,
            @RequestParam(required = false) String q) {
        return Paginacion.respuesta(productService.paginarProductos(Paginacion.busqueda(q),
                Paginacion.crear(page, size, Sort.by(Sort.Direction.DESC, "id"))));
    }

    @GetMapping("/api/datatables")
    public ResponseEntity<Map<String, Object>> listarParaDataTables() {
        try {
            List<ProductoResponse> productos = productService.listarTodosProductos().stream()
                    .map(ProductoResponse::from)
                    .toList();

            Map<String, Object> response = new HashMap<>();
            response.put("draw", 1);
            response.put("recordsTotal", productos.size());
            response.put("recordsFiltered", productos.size());
            response.put("data", productos);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ApiResponses.error("Error al cargar datos para la tabla: " + ApiResponses.mensajeSeguro(e));
        }
    }
    /**
     * Obtener estadísticas de productos (activos, inactivos, eliminados)
     */
    @GetMapping("/api/estadisticas")
    public ResponseEntity<Map<String, Object>> obtenerEstadisticas() {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", productService.obtenerEstadisticas());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ApiResponses.error("Error al obtener estadísticas: " + ApiResponses.mensajeSeguro(e));
        }
    }
    /**
     * Obtener productos disponibles para ventas (solo datos necesarios)
     */
    @GetMapping("/api/listarDisponibles")
    public ResponseEntity<List<Map<String, Object>>> listarProductosDisponibles() {
        try {
            List<Producto> productos = productService.listarProductos().stream()
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
    public ResponseEntity<Map<String, Object>> productosDestacados(
            @RequestParam(required = false) String genero) {
        try {
            List<Producto> productosEntidad;

            if (genero != null && !genero.isEmpty()) {
                Genero generoEnum = Genero.valueOf(genero.toUpperCase());
                productosEntidad = productService.listarDestacadosPorGenero(generoEnum);
            } else {
                productosEntidad = productService.listarDestacados();
            }
            List<ProductoResponse> productos = productosEntidad.stream().map(ProductoResponse::from).toList();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", productos);
            response.put("total", productos.size());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ApiResponses.error("Género no válido");
        } catch (Exception e) {
            return ApiResponses.error("Error al obtener destacados: " + ApiResponses.mensajeSeguro(e));
        }
    }

    /**
     * Obtener productos por género
     */
    @GetMapping("/api/genero/{genero}")
    public ResponseEntity<Map<String, Object>> productosPorGenero(
            @PathVariable String genero,
            @RequestParam(required = false, defaultValue = "false") Boolean todos) {
        try {
            Genero generoEnum = Genero.valueOf(genero.toUpperCase());
            List<ProductoResponse> productos = (todos
                ? productService.listarTodosPorGenero(generoEnum)
                : productService.listarPorGenero(generoEnum)).stream().map(ProductoResponse::from).toList();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", productos);
            response.put("total", productos.size());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ApiResponses.error("Género no válido");
        } catch (Exception e) {
            return ApiResponses.error("Error al filtrar por género: " + ApiResponses.mensajeSeguro(e));
        }
    }

    /**
     * Obtener productos en rebaja (SALE)
     */
    @GetMapping("/api/sale")
    public ResponseEntity<Map<String, Object>> productosEnRebaja(
            @RequestParam(required = false) String genero) {
        try {
            List<Producto> productosEntidad;

            if (genero != null && !genero.isEmpty()) {
                Genero generoEnum = Genero.valueOf(genero.toUpperCase());
                productosEntidad = productService.listarEnRebajaPorGenero(generoEnum);
            } else {
                productosEntidad = productService.listarEnRebaja();
            }
            List<ProductoResponse> productos = productosEntidad.stream().map(ProductoResponse::from).toList();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", productos);
            response.put("total", productos.size());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ApiResponses.error("Género no válido");
        } catch (Exception e) {
            return ApiResponses.error("Error al obtener productos en rebaja: " + ApiResponses.mensajeSeguro(e));
        }
    }

    // ===================== API REST - Por Categoría/Marca =====================

    /**
     * Obtener productos por categoría
     */
    @GetMapping("/api/categoria/{id}")
    public ResponseEntity<Map<String, Object>> productosPorCategoria(@PathVariable Long id) {
        try {
            List<ProductoResponse> productos = productService.listarPorCategoria(id).stream()
                    .map(ProductoResponse::from)
                    .toList();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", productos);
            response.put("total", productos.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ApiResponses.error("Error al filtrar por categoría: " + ApiResponses.mensajeSeguro(e));
        }
    }

    /**
     * Obtener productos por marca
     */
    @GetMapping("/api/marca/{id}")
    public ResponseEntity<Map<String, Object>> productosPorMarca(@PathVariable Long id) {
        try {
            List<ProductoResponse> productos = productService.listarPorMarca(id).stream()
                    .map(ProductoResponse::from)
                    .toList();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", productos);
            response.put("total", productos.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ApiResponses.error("Error al filtrar por marca: " + ApiResponses.mensajeSeguro(e));
        }
    }

    // ===================== API REST - CRUD =====================

    /**
     * Obtener producto por ID
     */
    @GetMapping("/api/{id}")
    public ResponseEntity<Map<String, Object>> obtenerProducto(@PathVariable Long id) {
        try {
            Optional<Producto> producto = productService.obtenerProductoPorId(id);

            Map<String, Object> response = new HashMap<>();
            if (producto.isPresent()) {
                response.put("success", true);
                response.put("data", ProductoResponse.from(producto.get()));
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Producto no encontrado");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (Exception e) {
            return ApiResponses.error("Error al obtener el producto: " + ApiResponses.mensajeSeguro(e));
        }
    }

    /**
     * Crear nuevo producto
     */
    @PostMapping("/api/crear")
    public ResponseEntity<Map<String, Object>> crearProducto(@Valid @RequestBody ProductoRequest request, BindingResult result) {
        if (result.hasErrors()) {
            return ApiResponses.validationError(result);
        }

        try {
            if (productService.existeProducto(request.getNombre())) {
                return ApiResponses.error("Ya existe un producto con ese nombre");
            }

            Producto nuevoProducto = productService.guardarProducto(request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Producto creado exitosamente");
            response.put("data", ProductoResponse.from(nuevoProducto));

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (ProductoService.ProductoException e) {
            return ApiResponses.error(ApiResponses.mensajeSeguro(e));
        } catch (Exception e) {
            return ApiResponses.error("Error al crear el producto: " + ApiResponses.mensajeSeguro(e));
        }
    }

    /**
     * Actualizar producto existente
     */
    @PutMapping("/api/actualizar/{id}")
    public ResponseEntity<Map<String, Object>> actualizarProducto(
            @PathVariable Long id,
            @Valid @RequestBody ProductoRequest request,
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
            Producto productoActualizado = productService.guardarProducto(request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Producto actualizado exitosamente");
            response.put("data", ProductoResponse.from(productoActualizado));

            return ResponseEntity.ok(response);

        } catch (ProductoService.ProductoException e) {
            return ApiResponses.error(ApiResponses.mensajeSeguro(e));
        } catch (Exception e) {
            return ApiResponses.error("Error al actualizar el producto: " + ApiResponses.mensajeSeguro(e));
        }
    }

    /**
     * Eliminar producto (eliminación lógica)
     */
    @DeleteMapping("/api/eliminar/{id}")
    public ResponseEntity<Map<String, Object>> eliminarProducto(@PathVariable Long id) {
        try {
            productService.eliminarProducto(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Producto eliminado exitosamente");

            return ResponseEntity.ok(response);

        } catch (ProductoService.ProductoException e) {
            return ApiResponses.error(ApiResponses.mensajeSeguro(e));
        } catch (Exception e) {
            return ApiResponses.error("Error al eliminar el producto: " + ApiResponses.mensajeSeguro(e));
        }
    }

    /**
     * Cambiar estado de producto (activar/desactivar)
     */
    @PutMapping("/api/cambiar-estado/{id}")
    public ResponseEntity<Map<String, Object>> cambiarEstado(@PathVariable Long id) {
        try {
            Optional<Producto> producto = productService.cambiarEstadoProducto(id);

            Map<String, Object> response = new HashMap<>();
            if (producto.isPresent()) {
                response.put("success", true);
                response.put("message", "Estado cambiado exitosamente");
                response.put("data", ProductoResponse.from(producto.get()));
                return ResponseEntity.ok(response);
            } else {
                return ApiResponses.error("Producto no encontrado");
            }

        } catch (Exception e) {
            return ApiResponses.error("Error al cambiar el estado: " + ApiResponses.mensajeSeguro(e));
        }
    }

    /**
     * Cambiar estado destacado de producto
     */
    @PutMapping("/api/destacado/{id}")
    public ResponseEntity<Map<String, Object>> cambiarDestacado(@PathVariable Long id) {
        try {
            Optional<Producto> producto = productService.cambiarEstadoDestacado(id);

            Map<String, Object> response = new HashMap<>();
            if (producto.isPresent()) {
                response.put("success", true);
                response.put("message", "Estado destacado cambiado exitosamente");
                response.put("data", ProductoResponse.from(producto.get()));
                return ResponseEntity.ok(response);
            } else {
                return ApiResponses.error("Producto no encontrado");
            }

        } catch (Exception e) {
            return ApiResponses.error("Error al cambiar el destacado: " + ApiResponses.mensajeSeguro(e));
        }
    }

    // ===================== API REST - Búsquedas =====================

    /**
     * Buscar productos por texto
     */
    @GetMapping("/api/buscar")
    public ResponseEntity<Map<String, Object>> buscarProductos(
            @RequestParam String q,
            @RequestParam(required = false, defaultValue = "false") Boolean todos) {
        try {
            List<ProductoResponse> productos = (todos
                ? productService.buscarProductosTodos(q)
                : productService.buscarProductos(q)).stream().map(ProductoResponse::from).toList();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", productos);
            response.put("total", productos.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ApiResponses.error("Error en la búsqueda: " + ApiResponses.mensajeSeguro(e));
        }
    }

    /**
     * Buscar por rango de precio
     */
    @GetMapping("/api/rango-precio")
    public ResponseEntity<Map<String, Object>> buscarPorRangoPrecio(
            @RequestParam BigDecimal min,
            @RequestParam BigDecimal max) {
        try {
            List<ProductoResponse> productos = productService.buscarPorRangoPrecio(min, max).stream()
                    .map(ProductoResponse::from)
                    .toList();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", productos);
            response.put("total", productos.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ApiResponses.error("Error al buscar por precio: " + ApiResponses.mensajeSeguro(e));
        }
    }

    /**
     * Obtener productos recientes
     */
    @GetMapping("/api/recientes")
    public ResponseEntity<Map<String, Object>> productosRecientes() {
        try {
            List<ProductoResponse> productos = productService.obtenerProductosRecientes().stream()
                    .map(ProductoResponse::from)
                    .toList();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", productos);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ApiResponses.error("Error al obtener recientes: " + ApiResponses.mensajeSeguro(e));
        }
    }

    /**
     * Obtener destacados recientes
     */
    @GetMapping("/api/destacados-recientes")
    public ResponseEntity<Map<String, Object>> destacadosRecientes() {
        try {
            List<ProductoResponse> productos = productService.obtenerDestacadosRecientes().stream()
                    .map(ProductoResponse::from)
                    .toList();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", productos);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ApiResponses.error("Error al obtener destacados recientes: " + ApiResponses.mensajeSeguro(e));
        }
    }

    // ===================== API REST - Gestión de Stock =====================

    /**
     * Obtener productos con stock bajo
     */
    @GetMapping("/api/stock-bajo")
    public ResponseEntity<Map<String, Object>> productosStockBajo(
            @RequestParam(required = false, defaultValue = "10") Integer limite) {
        try {
            List<ProductoResponse> productos = productService.listarStockBajo(limite).stream()
                    .map(ProductoResponse::from)
                    .toList();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", productos);
            response.put("total", productos.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ApiResponses.error("Error al obtener productos con stock bajo: " + ApiResponses.mensajeSeguro(e));
        }
    }
    /**
     * Obtener productos con stock disponible
     */
    @GetMapping("/api/stock-disponible")
    public ResponseEntity<Map<String, Object>> productosStockDisponible(
            @RequestParam(required = false, defaultValue = "0") Integer minimo) {
        try {
            List<ProductoResponse> productos = productService.listarConStockDisponible(minimo).stream()
                    .map(ProductoResponse::from)
                    .toList();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", productos);
            response.put("total", productos.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ApiResponses.error("Error al obtener productos con stock: " + ApiResponses.mensajeSeguro(e));
        }
    }

    /**
     * Actualizar stock de un producto
     */
    @PutMapping("/api/stock/{id}")
    public ResponseEntity<Map<String, Object>> actualizarStock(
            @PathVariable Long id,
            @RequestParam Integer stock) {
        try {
            Optional<Producto> producto = productService.actualizarStock(id, stock);

            Map<String, Object> response = new HashMap<>();
            if (producto.isPresent()) {
                response.put("success", true);
                response.put("message", "Stock actualizado exitosamente");
                response.put("data", ProductoResponse.from(producto.get()));
                return ResponseEntity.ok(response);
            } else {
                return ApiResponses.error("No se pudo actualizar el stock");
            }
        } catch (Exception e) {
            return ApiResponses.error("Error al actualizar stock: " + ApiResponses.mensajeSeguro(e));
        }
    }

}