package com.example.acceso.repository;

import com.example.acceso.model.Brand;
import com.example.acceso.model.Category;
import com.example.acceso.model.Genero;
import com.example.acceso.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByNombre(String nombre);
    boolean existsByNombreAndEstadoNot(String nombre, Integer estado);
    boolean existsByNombreAndIdNotAndEstadoNot(String nombre, Long id, Integer estado);

    List<Product> findAllByEstado(Integer estado);
    List<Product> findAllByEstadoOrderByNombreAsc(Integer estado);
    long countByEstado(Integer estado);

    // ===================== Filtros para Navbar =====================
    List<Product> findByImagenIsNotNullAndEstado(Integer estado);
    List<Product> findByImagenIsNullAndEstado(Integer estado);

    // Destacados
    List<Product> findByDestacadoTrueAndEstadoAndCategory_NombreIgnoreCase(Integer estado, String nombreCategoria);
    List<Product> findByDestacadoTrueAndEstado(Integer estado);
    List<Product> findByDestacadoTrue();

    // Por género
    List<Product> findByGeneroAndEstado(Genero genero, Integer estado);
    List<Product> findByGenero(Genero genero);

    // SALE (productos en rebaja)
    List<Product> findByDescuentoGreaterThanAndEstado(BigDecimal descuento, Integer estado);
    List<Product> findByDescuentoGreaterThan(BigDecimal descuento);

    // Combinaciones para filtros múltiples
    List<Product> findByGeneroAndDestacadoTrueAndEstado(Genero genero, Integer estado);
    List<Product> findByGeneroAndDescuentoGreaterThanAndEstado(Genero genero, BigDecimal descuento, Integer estado);

    // ===================== Búsquedas por relaciones =====================

    List<Product> findByCategoryAndEstado(Category category, Integer estado);
    List<Product> findByCategory_IdAndEstado(Long categoryId, Integer estado);
    long countByCategory(Category category);

    List<Product> findByBrandAndEstado(Brand brand, Integer estado);
    List<Product> findByBrand_IdAndEstado(Long brandId, Integer estado);
    long countByBrand(Brand brand);

    // ===================== Búsquedas por texto =====================

    List<Product> findByNombreContainingIgnoreCaseAndEstado(String nombre, Integer estado);
    List<Product> findByNombreContainingIgnoreCase(String nombre);
    List<Product> findByNombreContainingIgnoreCaseOrDescripcionContainingIgnoreCaseAndEstado(
        String nombre, String descripcion, Integer estado);

    // ===================== Filtros avanzados =====================

    List<Product> findByPrecioBetweenAndEstado(BigDecimal precioMin, BigDecimal precioMax, Integer estado);

    List<Product> findTop10ByEstadoOrderByFechaCreacionDesc(Integer estado);
    List<Product> findTop10ByDestacadoTrueAndEstadoOrderByFechaCreacionDesc(Integer estado);

    // ===================== Gestión de Stock ===================== //

    List<Product> findByStockAndEstado(Integer stock, Integer estado);
    List<Product> findByStockGreaterThanAndEstado(Integer stock, Integer estado);
    List<Product> findByStockLessThanEqualAndEstado(Integer stock, Integer estado);

    @Query("SELECT DISTINCT p FROM Product p " +
           "LEFT JOIN FETCH p.category " +
           "LEFT JOIN FETCH p.brand " +
           "LEFT JOIN FETCH p.imagenes")
    List<Product> findAllWithRelations();

    
}
