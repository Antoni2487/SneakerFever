package com.example.acceso.product;

import com.example.acceso.brand.Brand;
import com.example.acceso.category.Category;
import org.springframework.data.jpa.repository.EntityGraph;
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

    @EntityGraph(attributePaths = {"category", "brand", "imagenes"})
    List<Product> findAllByEstado(Integer estado);
    @EntityGraph(attributePaths = {"category", "brand", "imagenes"})
    List<Product> findAllByEstadoOrderByNombreAsc(Integer estado);
    long countByEstado(Integer estado);

    // ===================== Filtros para Navbar =====================
    @EntityGraph(attributePaths = {"category", "brand", "imagenes"})
    List<Product> findByImagenIsNotNullAndEstado(Integer estado);
    @EntityGraph(attributePaths = {"category", "brand", "imagenes"})
    List<Product> findByImagenIsNullAndEstado(Integer estado);

    // Destacados
    @EntityGraph(attributePaths = {"category", "brand", "imagenes"})
    List<Product> findByDestacadoTrueAndEstadoAndCategory_NombreIgnoreCase(Integer estado, String nombreCategoria);
    @EntityGraph(attributePaths = {"category", "brand", "imagenes"})
    List<Product> findByDestacadoTrueAndEstado(Integer estado);
    @EntityGraph(attributePaths = {"category", "brand", "imagenes"})
    List<Product> findByDestacadoTrue();

    // Por género
    @EntityGraph(attributePaths = {"category", "brand", "imagenes"})
    List<Product> findByGeneroAndEstado(Genero genero, Integer estado);
    @EntityGraph(attributePaths = {"category", "brand", "imagenes"})
    List<Product> findByGenero(Genero genero);

    // SALE (productos en rebaja)
    @EntityGraph(attributePaths = {"category", "brand", "imagenes"})
    List<Product> findByDescuentoGreaterThanAndEstado(BigDecimal descuento, Integer estado);
    @EntityGraph(attributePaths = {"category", "brand", "imagenes"})
    List<Product> findByDescuentoGreaterThan(BigDecimal descuento);

    // Combinaciones para filtros múltiples
    @EntityGraph(attributePaths = {"category", "brand", "imagenes"})
    List<Product> findByGeneroAndDestacadoTrueAndEstado(Genero genero, Integer estado);
    @EntityGraph(attributePaths = {"category", "brand", "imagenes"})
    List<Product> findByGeneroAndDescuentoGreaterThanAndEstado(Genero genero, BigDecimal descuento, Integer estado);

    // ===================== Búsquedas por relaciones =====================

    @EntityGraph(attributePaths = {"category", "brand", "imagenes"})
    List<Product> findByCategoryAndEstado(Category category, Integer estado);
    @EntityGraph(attributePaths = {"category", "brand", "imagenes"})
    List<Product> findByCategory_IdAndEstado(Long categoryId, Integer estado);
    long countByCategory(Category category);

    @EntityGraph(attributePaths = {"category", "brand", "imagenes"})
    List<Product> findByBrandAndEstado(Brand brand, Integer estado);
    @EntityGraph(attributePaths = {"category", "brand", "imagenes"})
    List<Product> findByBrand_IdAndEstado(Long brandId, Integer estado);
    long countByBrand(Brand brand);

    // ===================== Búsquedas por texto =====================

    @EntityGraph(attributePaths = {"category", "brand", "imagenes"})
    List<Product> findByNombreContainingIgnoreCaseAndEstado(String nombre, Integer estado);
    @EntityGraph(attributePaths = {"category", "brand", "imagenes"})
    List<Product> findByNombreContainingIgnoreCase(String nombre);
    @EntityGraph(attributePaths = {"category", "brand", "imagenes"})
    List<Product> findByNombreContainingIgnoreCaseOrDescripcionContainingIgnoreCaseAndEstado(
        String nombre, String descripcion, Integer estado);

    // ===================== Filtros avanzados =====================

    @EntityGraph(attributePaths = {"category", "brand", "imagenes"})
    List<Product> findByPrecioBetweenAndEstado(BigDecimal precioMin, BigDecimal precioMax, Integer estado);

    @EntityGraph(attributePaths = {"category", "brand", "imagenes"})
    List<Product> findTop10ByEstadoOrderByFechaCreacionDesc(Integer estado);
    @EntityGraph(attributePaths = {"category", "brand", "imagenes"})
    List<Product> findTop10ByDestacadoTrueAndEstadoOrderByFechaCreacionDesc(Integer estado);

    // ===================== Gestión de Stock ===================== //

    @EntityGraph(attributePaths = {"category", "brand", "imagenes"})
    List<Product> findByStockAndEstado(Integer stock, Integer estado);
    @EntityGraph(attributePaths = {"category", "brand", "imagenes"})
    List<Product> findByStockGreaterThanAndEstado(Integer stock, Integer estado);
    @EntityGraph(attributePaths = {"category", "brand", "imagenes"})
    List<Product> findByStockLessThanEqualAndEstado(Integer stock, Integer estado);

    @Query("SELECT DISTINCT p FROM Product p " +
           "LEFT JOIN FETCH p.category " +
           "LEFT JOIN FETCH p.brand " +
           "LEFT JOIN FETCH p.imagenes")
    List<Product> findAllWithRelations();

}
