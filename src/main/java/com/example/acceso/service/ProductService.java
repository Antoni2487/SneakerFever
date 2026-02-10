package com.example.acceso.service;

import com.example.acceso.model.Brand;
import com.example.acceso.model.Category;
import com.example.acceso.model.Genero;
import com.example.acceso.model.Product;
import com.example.acceso.repository.ProductRepository;
import com.example.acceso.repository.RepositorioVentas.DetalleVentaRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final DetalleVentaRepository detalleVentaRepository;

    public ProductService(ProductRepository productRepository, DetalleVentaRepository detalleVentaRepository) {
        this.productRepository = productRepository;
        this.detalleVentaRepository = detalleVentaRepository;
    }

    // ===================== Listados =====================
    @Transactional(readOnly = true)
    public List<Product> listarProductos() {
        return productRepository.findAllByEstado(1);
    }

    @Transactional(readOnly = true)
    public List<Product> listarTodosProductos() {
        return productRepository.findAllWithRelations();
    }

    @Transactional(readOnly = true)
    public List<Product> listarProductosOrdenados() {
        return productRepository.findAllByEstadoOrderByNombreAsc(1);
    }

    // ===================== Filtros para Navbar =====================

    @Transactional(readOnly = true)
    public List<Product> listarDestacados() {
        return productRepository.findByDestacadoTrueAndEstado(1);
    }

    @Transactional(readOnly = true)
    public List<Product> obtenerDestacadosPorCategoria(String nombreCategoria) {
        return productRepository.findByDestacadoTrueAndEstadoAndCategory_NombreIgnoreCase(1, nombreCategoria);
    }

    @Transactional(readOnly = true)
    public List<Product> listarTodosDestacados() {
        return productRepository.findByDestacadoTrue();
    }

    @Transactional(readOnly = true)
    public List<Product> listarPorGenero(Genero genero) {
        return productRepository.findByGeneroAndEstado(genero, 1);
    }

    @Transactional(readOnly = true)
    public List<Product> listarTodosPorGenero(Genero genero) {
        return productRepository.findByGenero(genero);
    }

    @Transactional(readOnly = true)
    public List<Product> listarEnRebaja() {
        return productRepository.findByDescuentoGreaterThanAndEstado(BigDecimal.ZERO, 1);
    }

    @Transactional(readOnly = true)
    public List<Product> listarTodosEnRebaja() {
        return productRepository.findByDescuentoGreaterThan(BigDecimal.ZERO);
    }

    @Transactional(readOnly = true)
    public List<Product> listarPorCategoria(Long categoryId) {
        return productRepository.findByCategory_IdAndEstado(categoryId, 1);
    }

    @Transactional(readOnly = true)
    public List<Product> listarPorMarca(Long brandId) {
        return productRepository.findByBrand_IdAndEstado(brandId, 1);
    }

    // ===================== Filtros combinados =====================

    @Transactional(readOnly = true)
    public List<Product> listarDestacadosPorGenero(Genero genero) {
        return productRepository.findByGeneroAndDestacadoTrueAndEstado(genero, 1);
    }

    @Transactional(readOnly = true)
    public List<Product> listarEnRebajaPorGenero(Genero genero) {
        return productRepository.findByGeneroAndDescuentoGreaterThanAndEstado(genero, BigDecimal.ZERO, 1);
    }
    // Agregar este método al final de la clase (antes del cierre):
    @Transactional(readOnly = true)
    public List<Product> obtenerZapatillasMasVendidas() {
        List<Object[]> resultados = detalleVentaRepository
            .findTop5ProductosMasVendidosPorCategoria("Zapatillas");
        
        // Convertir resultados a List<Product>
        List<Product> zapatillasMasVendidas = new ArrayList<>();
        for (int i = 0; i < Math.min(5, resultados.size()); i++) {
            Product producto = (Product) resultados.get(i)[0];
            zapatillasMasVendidas.add(producto);
        }
        
        // Si no hay suficientes ventas, completar con destacadas
        if (zapatillasMasVendidas.size() < 3) {
            List<Product> destacadas = productRepository
                .findByDestacadoTrueAndEstadoAndCategory_NombreIgnoreCase(1, "Zapatillas");
            
            for (Product destacada : destacadas) {
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
    public Product guardarProducto(Product producto) {
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
                Optional<Product> existente = obtenerProductoPorId(producto.getId());
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
    public long contarPorCategoria(Category category) {
        return productRepository.countByCategory(category);
    }

    @Transactional(readOnly = true)
    public long contarPorMarca(Brand brand) {
        return productRepository.countByBrand(brand);
    }

    public List<Product> findAll() {
    return productRepository.findAll();
   }

    // ===================== Obtención =====================
    @Transactional(readOnly = true)
    public Optional<Product> obtenerProductoPorId(Long id) {
        if (id == null || id <= 0) return Optional.empty();
        return productRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Product> findByNombre(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) return Optional.empty();
        return productRepository.findByNombre(nombre.trim());
    }
    
    

    // ===================== Eliminación / Estado =====================
    @Transactional
    public void eliminarProducto(Long id) {
        Product producto = obtenerProductoPorId(id)
                .orElseThrow(() -> new ProductoException("Producto no encontrado"));
        producto.setEstado(2);
        productRepository.save(producto);
    }

    @Transactional
    public Optional<Product> cambiarEstadoProducto(Long id) {
        if (id == null || id <= 0) return Optional.empty();
        return obtenerProductoPorId(id).map(p -> {
            p.setEstado(p.getEstado() == 1 ? 0 : 1);
            return productRepository.save(p);
        });
    }

    @Transactional
    public Optional<Product> cambiarEstadoDestacado(Long id) {
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
    public List<Product> buscarProductos(String texto) {
        if (texto == null || texto.trim().isEmpty()) return listarProductos();
        return productRepository.findByNombreContainingIgnoreCaseOrDescripcionContainingIgnoreCaseAndEstado(
                texto.trim(), texto.trim(), 1
        );
    }

    @Transactional(readOnly = true)
    public List<Product> buscarProductosTodos(String texto) {
        if (texto == null || texto.trim().isEmpty()) return listarTodosProductos();
        return productRepository.findByNombreContainingIgnoreCase(texto.trim());
    }

    @Transactional(readOnly = true)
    public List<Product> buscarPorRangoPrecio(BigDecimal precioMin, BigDecimal precioMax) {
        return productRepository.findByPrecioBetweenAndEstado(precioMin, precioMax, 1);
    }

    @Transactional(readOnly = true)
    public List<Product> obtenerProductosRecientes() {
        return productRepository.findTop10ByEstadoOrderByFechaCreacionDesc(1);
    }

    @Transactional(readOnly = true)
    public List<Product> obtenerDestacadosRecientes() {
        return productRepository.findTop10ByDestacadoTrueAndEstadoOrderByFechaCreacionDesc(1);
    }

    // ===================== Gestión de Stock =====================
    @Transactional(readOnly = true)
    public List<Product> listarPorStock(Integer stock) {
        return productRepository.findByStockAndEstado(stock, 1);
    }

    @Transactional(readOnly = true)
    public List<Product> listarConStockDisponible(Integer stockMinimo) {
        return productRepository.findByStockGreaterThanAndEstado(stockMinimo, 1);
    }

    @Transactional(readOnly = true)
    public List<Product> listarStockBajo(Integer stockMaximo) {
        return productRepository.findByStockLessThanEqualAndEstado(stockMaximo,  1);
    }

    @Transactional
    public Optional<Product> actualizarStock(Long id, Integer nuevoStock) {
        if (id == null || id <= 0 || nuevoStock == null || nuevoStock < 0) {
            return Optional.empty();
        }
        return obtenerProductoPorId(id).map(p -> {
            p.setStock(nuevoStock);
            return productRepository.save(p);
        });
    }
    @Transactional(readOnly = true)
    public List<Product> obtenerTopProductos() {
        List<Product> productosConDescuento = productRepository
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
    public Product actualizarImagen(Long id, String imagenUrl) {
        if (id == null || id <= 0) {
            throw new ProductoException("ID de producto inválido");
        }

        if (imagenUrl == null || imagenUrl.trim().isEmpty()) {
            throw new ProductoException("La URL de la imagen no puede estar vacía");
        }

        Product producto = obtenerProductoPorId(id)
                .orElseThrow(() -> new ProductoException("Producto no encontrado con ID: " + id));

        producto.setImagen(imagenUrl.trim());  // ← CAMBIO AQUÍ
        return productRepository.save(producto);
    }

    /**
     * Elimina la imagen de un producto
     */
    @Transactional
    public Product eliminarImagen(Long id) {
        if (id == null || id <= 0) {
            throw new ProductoException("ID de producto inválido");
        }

        Product producto = obtenerProductoPorId(id)
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
                .map(Product::getImagen)  // ← CAMBIO AQUÍ
                .filter(url -> url != null && !url.trim().isEmpty());
    }

    /**
     * Lista productos que tienen imagen
     */
    @Transactional(readOnly = true)
    public List<Product> listarProductosConImagen() {
        return productRepository.findByImagenIsNotNullAndEstado(1);
    }

    /**
     * Lista productos sin imagen
     */
    @Transactional(readOnly = true)
    public List<Product> listarProductosSinImagen() {
        return productRepository.findByImagenIsNullAndEstado(1);
    }

    // ===================== Validaciones =====================
    private void validarProducto(Product producto) {
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

