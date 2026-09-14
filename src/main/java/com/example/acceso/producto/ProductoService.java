package com.example.acceso.producto;

import com.example.acceso.marca.Marca;
import com.example.acceso.marca.MarcaRepository;
import com.example.acceso.categoria.Categoria;
import com.example.acceso.categoria.CategoriaRepository;
import com.example.acceso.venta.DetalleVentaRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProductoService {

    private final ProductoRepository productRepository;
    private final DetalleVentaRepository detalleVentaRepository;
    private final CategoriaRepository categoryRepository;
    private final MarcaRepository brandRepository;

    public ProductoService(ProductoRepository productRepository,
                           DetalleVentaRepository detalleVentaRepository,
                           CategoriaRepository categoryRepository,
                           MarcaRepository brandRepository) {
        this.productRepository = productRepository;
        this.detalleVentaRepository = detalleVentaRepository;
        this.categoryRepository = categoryRepository;
        this.brandRepository = brandRepository;
    }

    // ===================== Listados =====================
    @Transactional(readOnly = true)
    public List<Producto> listarProductos() {
        return productRepository.findAllByEstado(1);
    }

    @Transactional(readOnly = true)
    public List<Producto> listarTodosProductos() {
        return productRepository.findAllWithRelations();
    }

    @Transactional(readOnly = true)
    public List<Producto> listarProductosOrdenados() {
        return productRepository.findAllByEstadoOrderByNombreAsc(1);
    }

    // ===================== Filtros para Navbar =====================

    @Transactional(readOnly = true)
    public List<Producto> listarDestacados() {
        return productRepository.findByDestacadoTrueAndEstado(1);
    }

    @Transactional(readOnly = true)
    public List<Producto> obtenerDestacadosPorCategoria(String nombreCategoria) {
        return productRepository.findByDestacadoTrueAndEstadoAndCategory_NombreIgnoreCase(1, nombreCategoria);
    }

    @Transactional(readOnly = true)
    public List<Producto> listarTodosDestacados() {
        return productRepository.findByDestacadoTrue();
    }

    @Transactional(readOnly = true)
    public List<Producto> listarPorGenero(Genero genero) {
        return productRepository.findByGeneroAndEstado(genero, 1);
    }

    @Transactional(readOnly = true)
    public List<Producto> listarTodosPorGenero(Genero genero) {
        return productRepository.findByGenero(genero);
    }

    @Transactional(readOnly = true)
    public List<Producto> listarEnRebaja() {
        return productRepository.findByDescuentoGreaterThanAndEstado(BigDecimal.ZERO, 1);
    }

    @Transactional(readOnly = true)
    public List<Producto> listarTodosEnRebaja() {
        return productRepository.findByDescuentoGreaterThan(BigDecimal.ZERO);
    }

    @Transactional(readOnly = true)
    public List<Producto> listarPorCategoria(Long categoryId) {
        return productRepository.findByCategory_IdAndEstado(categoryId, 1);
    }

    @Transactional(readOnly = true)
    public List<Producto> listarPorMarca(Long brandId) {
        return productRepository.findByBrand_IdAndEstado(brandId, 1);
    }

    // ===================== Filtros combinados =====================

    @Transactional(readOnly = true)
    public List<Producto> listarDestacadosPorGenero(Genero genero) {
        return productRepository.findByGeneroAndDestacadoTrueAndEstado(genero, 1);
    }

    @Transactional(readOnly = true)
    public List<Producto> listarEnRebajaPorGenero(Genero genero) {
        return productRepository.findByGeneroAndDescuentoGreaterThanAndEstado(genero, BigDecimal.ZERO, 1);
    }
    // Agregar este método al final de la clase (antes del cierre):
    @Transactional(readOnly = true)
    public List<Producto> obtenerZapatillasMasVendidas() {
        List<Object[]> resultados = detalleVentaRepository
            .findTop5ProductosMasVendidosPorCategoria("Zapatillas");
        
        // Convertir resultados a List<Producto>
        List<Producto> zapatillasMasVendidas = new ArrayList<>();
        for (int i = 0; i < Math.min(5, resultados.size()); i++) {
            Producto producto = (Producto) resultados.get(i)[0];
            zapatillasMasVendidas.add(producto);
        }
        
        // Si no hay suficientes ventas, completar con destacadas
        if (zapatillasMasVendidas.size() < 3) {
            List<Producto> destacadas = productRepository
                .findByDestacadoTrueAndEstadoAndCategory_NombreIgnoreCase(1, "Zapatillas");
            
            for (Producto destacada : destacadas) {
                if (zapatillasMasVendidas.size() >= 5) break;
                if (!zapatillasMasVendidas.contains(destacada)) {
                    zapatillasMasVendidas.add(destacada);
                }
            }
        }
    
    return zapatillasMasVendidas;
}

    // ===================== Guardar / Actualizar =====================
    @Transactional
    public Producto guardarProducto(Producto producto) {
        validarProducto(producto);

        try {
            // Normalizar texto
            producto.setNombre(producto.getNombre().trim());
            if (producto.getDescripcion() != null) {
                producto.setDescripcion(producto.getDescripcion().trim());
                if (producto.getDescripcion().isEmpty()) {
                    producto.setDescripcion(null);
                }
            }

            // Validar descuento
            if (producto.getDescuento() != null) {
                if (producto.getDescuento().compareTo(BigDecimal.ZERO) < 0 ||
                        producto.getDescuento().compareTo(BigDecimal.valueOf(100)) > 0) {
                    throw new ProductoException("El descuento debe estar entre 0 y 100");
                }
                if (producto.getDescuento().compareTo(BigDecimal.ZERO) == 0) {
                    producto.setDescuento(null);
                }
            }

            // Validar precio
            if (producto.getPrecio().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ProductoException("El precio debe ser mayor a 0");
            }

            // Validar stock
            if (producto.getStock() != null && producto.getStock() < 0) {
                throw new ProductoException("El stock no puede ser negativo");
            }

            if (producto.getId() != null) {
                // Actualización
                Optional<Producto> existente = obtenerProductoPorId(producto.getId());
                if (existente.isPresent()) {
                    if (producto.getEstado() == null) {
                        producto.setEstado(existente.get().getEstado());
                    }
                    if (producto.getDestacado() == null) {
                        producto.setDestacado(existente.get().getDestacado());
                    }
                } else {
                    throw new ProductoException("Producto no encontrado para actualizar");
                }
            } else {
                // Nuevo producto
                if (producto.getEstado() == null) {
                    producto.setEstado(1);
                }
                if (producto.getDestacado() == null) {
                    producto.setDestacado(false);
                }
            }

            return productRepository.save(producto);

        } catch (DataIntegrityViolationException e) {
            String msg = e.getMessage().toLowerCase();
            if (msg.contains("nombre") || msg.contains("unique")) {
                throw new ProductoException("Ya existe un producto con ese nombre");
            }
            throw new ProductoException("Error de integridad de datos");
        } catch (Exception e) {
            throw new ProductoException("Error al guardar el producto: " + e.getMessage(), e);
        }
    }

    @Transactional
    public Producto guardarProducto(ProductoRequest request) {
        Categoria category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ProductoException("Categoría no encontrada"));
        Marca brand = brandRepository.findById(request.getBrandId())
                .orElseThrow(() -> new ProductoException("Marca no encontrada"));

        Producto producto = request.getId() != null
                ? obtenerProductoPorId(request.getId())
                        .orElseThrow(() -> new ProductoException("Producto no encontrado para actualizar"))
                : new Producto();

        producto.setNombre(request.getNombre());
        producto.setDescripcion(request.getDescripcion());
        producto.setGenero(request.getGenero());
        producto.setPrecio(request.getPrecio());
        producto.setDescuento(request.getDescuento());
        producto.setStock(request.getStock());
        producto.setStockMinimo(request.getStockMinimo());
        producto.setCategory(category);
        producto.setBrand(brand);

        if (request.getDestacado() != null) {
            producto.setDestacado(request.getDestacado());
        }
        if (request.getEstado() != null) {
            producto.setEstado(request.getEstado());
        }

        // Sincronizar imagen/imagenes (antes duplicado a mano en crear y actualizar del controller)
        if (request.getImagenes() != null && !request.getImagenes().isEmpty()) {
            producto.setImagenes(new ArrayList<>(request.getImagenes()));
            producto.setImagen(request.getImagenes().get(0));
        } else if (request.getImagen() != null && !request.getImagen().isEmpty()) {
            producto.setImagen(request.getImagen());
            producto.setImagenes(List.of(request.getImagen()));
        }

        if (request.getId() != null) {
            producto.setId(request.getId());
        }

        return guardarProducto(producto);
    }

    // ===================== Contadores =====================
    @Transactional(readOnly = true)
    public long contarProductos() {
        return productRepository.countByEstado(1);
    }

    @Transactional(readOnly = true)
    public long contarTodosProductos() {
        return productRepository.count();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> obtenerEstadisticas() {
        long activos = productRepository.countByEstado(1);
        long inactivos = productRepository.countByEstado(0);
        long eliminados = productRepository.countByEstado(2);

        Map<String, Object> stats = new HashMap<>();
        stats.put("activos", activos);
        stats.put("inactivos", inactivos);
        stats.put("eliminados", eliminados);
        stats.put("total", activos + inactivos + eliminados);
        return stats;
    }

    @Transactional(readOnly = true)
    public long contarPorCategoria(Categoria category) {
        return productRepository.countByCategory(category);
    }

    @Transactional(readOnly = true)
    public long contarPorMarca(Marca brand) {
        return productRepository.countByBrand(brand);
    }

    public List<Producto> findAll() {
    return productRepository.findAll();
   }

    // ===================== Obtención =====================
    @Transactional(readOnly = true)
    public Optional<Producto> obtenerProductoPorId(Long id) {
        if (id == null || id <= 0) return Optional.empty();
        return productRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Producto> findByNombre(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) return Optional.empty();
        return productRepository.findByNombre(nombre.trim());
    }
    
    

    // ===================== Eliminación / Estado =====================
    @Transactional
    public void eliminarProducto(Long id) {
        Producto producto = obtenerProductoPorId(id)
                .orElseThrow(() -> new ProductoException("Producto no encontrado"));
        producto.setEstado(2);
        productRepository.save(producto);
    }

    @Transactional
    public Optional<Producto> cambiarEstadoProducto(Long id) {
        if (id == null || id <= 0) return Optional.empty();
        return obtenerProductoPorId(id).map(p -> {
            p.setEstado(p.getEstado() == 1 ? 0 : 1);
            return productRepository.save(p);
        });
    }

    @Transactional
    public Optional<Producto> cambiarEstadoDestacado(Long id) {
        if (id == null || id <= 0) return Optional.empty();
        return obtenerProductoPorId(id).map(p -> {
            p.setDestacado(!p.getDestacado());
            return productRepository.save(p);
        });
    }

    // ===================== Existencias =====================
    @Transactional(readOnly = true)
    public boolean existeProducto(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) return false;
        return productRepository.existsByNombreAndEstadoNot(nombre.trim(), 2);
    }

    @Transactional(readOnly = true)
    public boolean existeProductoParaActualizar(String nombre, Long id) {
        if (nombre == null || nombre.trim().isEmpty()) return false;
        return productRepository.existsByNombreAndIdNotAndEstadoNot(nombre.trim(), id, 2);
    }

    // ===================== Búsquedas =====================
    @Transactional(readOnly = true)
    public List<Producto> buscarProductos(String texto) {
        if (texto == null || texto.trim().isEmpty()) return listarProductos();
        return productRepository.findByNombreContainingIgnoreCaseOrDescripcionContainingIgnoreCaseAndEstado(
                texto.trim(), texto.trim(), 1
        );
    }

    @Transactional(readOnly = true)
    public List<Producto> buscarProductosTodos(String texto) {
        if (texto == null || texto.trim().isEmpty()) return listarTodosProductos();
        return productRepository.findByNombreContainingIgnoreCase(texto.trim());
    }

    @Transactional(readOnly = true)
    public List<Producto> buscarPorRangoPrecio(BigDecimal precioMin, BigDecimal precioMax) {
        return productRepository.findByPrecioBetweenAndEstado(precioMin, precioMax, 1);
    }

    @Transactional(readOnly = true)
    public List<Producto> obtenerProductosRecientes() {
        return productRepository.findTop10ByEstadoOrderByFechaCreacionDesc(1);
    }

    @Transactional(readOnly = true)
    public List<Producto> obtenerDestacadosRecientes() {
        return productRepository.findTop10ByDestacadoTrueAndEstadoOrderByFechaCreacionDesc(1);
    }

    // ===================== Gestión de Stock =====================
    @Transactional(readOnly = true)
    public List<Producto> listarPorStock(Integer stock) {
        return productRepository.findByStockAndEstado(stock, 1);
    }

    @Transactional(readOnly = true)
    public List<Producto> listarConStockDisponible(Integer stockMinimo) {
        return productRepository.findByStockGreaterThanAndEstado(stockMinimo, 1);
    }

    @Transactional(readOnly = true)
    public List<Producto> listarStockBajo(Integer stockMaximo) {
        return productRepository.findByStockLessThanEqualAndEstado(stockMaximo,  1);
    }

    @Transactional
    public Optional<Producto> actualizarStock(Long id, Integer nuevoStock) {
        if (id == null || id <= 0 || nuevoStock == null || nuevoStock < 0) {
            return Optional.empty();
        }
        return obtenerProductoPorId(id).map(p -> {
            p.setStock(nuevoStock);
            return productRepository.save(p);
        });
    }
    @Transactional(readOnly = true)
    public List<Producto> obtenerTopProductos() {
        List<Producto> productosConDescuento = productRepository
                .findByDescuentoGreaterThanAndEstado(BigDecimal.ZERO, 1);

        if (productosConDescuento == null || productosConDescuento.isEmpty()) {
            productosConDescuento = productRepository.findByDestacadoTrueAndEstado(1);
        }

        // Filtrar nulos por seguridad
        return productosConDescuento.stream()
                .filter(p -> p != null && p.getNombre() != null)
                .limit(5)
                .collect(Collectors.toList());
    }

    // ===================== Gestión de Imágenes =====================

    /**
     * Actualiza la URL de la imagen de un producto
     */
    @Transactional
    public Producto actualizarImagen(Long id, String imagenUrl) {
        if (id == null || id <= 0) {
            throw new ProductoException("ID de producto inválido");
        }

        if (imagenUrl == null || imagenUrl.trim().isEmpty()) {
            throw new ProductoException("La URL de la imagen no puede estar vacía");
        }

        Producto producto = obtenerProductoPorId(id)
                .orElseThrow(() -> new ProductoException("Producto no encontrado con ID: " + id));

        producto.setImagen(imagenUrl.trim());  // ← CAMBIO AQUÍ
        return productRepository.save(producto);
    }

    /**
     * Elimina la imagen de un producto
     */
    @Transactional
    public Producto eliminarImagen(Long id) {
        if (id == null || id <= 0) {
            throw new ProductoException("ID de producto inválido");
        }

        Producto producto = obtenerProductoPorId(id)
                .orElseThrow(() -> new ProductoException("Producto no encontrado con ID: " + id));

        producto.setImagen(null);  // ← CAMBIO AQUÍ
        return productRepository.save(producto);
    }

    /**
     * Obtiene la URL de la imagen de un producto
     */
    @Transactional(readOnly = true)
    public Optional<String> obtenerImagenUrl(Long id) {
        if (id == null || id <= 0) return Optional.empty();

        return obtenerProductoPorId(id)
                .map(Producto::getImagen)  // ← CAMBIO AQUÍ
                .filter(url -> url != null && !url.trim().isEmpty());
    }

    /**
     * Lista productos que tienen imagen
     */
    @Transactional(readOnly = true)
    public List<Producto> listarProductosConImagen() {
        return productRepository.findByImagenIsNotNullAndEstado(1);
    }

    /**
     * Lista productos sin imagen
     */
    @Transactional(readOnly = true)
    public List<Producto> listarProductosSinImagen() {
        return productRepository.findByImagenIsNullAndEstado(1);
    }

    // ===================== Validaciones =====================
    private void validarProducto(Producto producto) {
        if (producto == null) {
            throw new ProductoException("Producto no puede ser nulo");
        }
        if (producto.getNombre() == null || producto.getNombre().trim().isEmpty()) {
            throw new ProductoException("El nombre del producto es obligatorio");
        }
        if (producto.getPrecio() == null) {
            throw new ProductoException("El precio es obligatorio");
        }
        if (producto.getGenero() == null) {
            throw new ProductoException("El género es obligatorio");
        }
        if (producto.getCategory() == null) {
            throw new ProductoException("La categoría es obligatoria");
        }
        if (producto.getBrand() == null) {
            throw new ProductoException("La marca es obligatoria");
        }
    }

    // ===================== Excepción personalizada =====================
    public static class ProductoException extends RuntimeException {
        public ProductoException(String message) {
            super(message);
        }
        public ProductoException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

