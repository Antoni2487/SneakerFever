package com.example.acceso.producto;

import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import com.example.acceso.marca.Marca;
import com.example.acceso.categoria.Categoria;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {

    Optional<Producto> findByNombre(String nombre);
    boolean existsByNombreAndEstadoNot(String nombre, Integer estado);
    boolean existsByNombreAndIdNotAndEstadoNot(String nombre, Long id, Integer estado);

    @EntityGraph(attributePaths = {"category", "brand", "imagenes"})
    List<Producto> findAllByEstado(Integer estado);
    @EntityGraph(attributePaths = {"category", "brand", "imagenes"})
    List<Producto> findAllByEstadoOrderByNombreAsc(Integer estado);
    long countByEstado(Integer estado);

    // ===================== Filtros para Navbar =====================
    @EntityGraph(attributePaths = {"category", "brand", "imagenes"})
    List<Producto> findByImagenIsNotNullAndEstado(Integer estado);
    @EntityGraph(attributePaths = {"category", "brand", "imagenes"})
    List<Producto> findByImagenIsNullAndEstado(Integer estado);

    // Destacados
    @EntityGraph(attributePaths = {"category", "brand", "imagenes"})
    List<Producto> findByDestacadoTrueAndEstadoAndCategory_NombreIgnoreCase(Integer estado, String nombreCategoria);
    @EntityGraph(attributePaths = {"category", "brand", "imagenes"})
    List<Producto> findByDestacadoTrueAndEstado(Integer estado);
    @EntityGraph(attributePaths = {"category", "brand", "imagenes"})
    List<Producto> findByDestacadoTrue();

    // Por género
    @EntityGraph(attributePaths = {"category", "brand", "imagenes"})
    List<Producto> findByGeneroAndEstado(Genero genero, Integer estado);
    @EntityGraph(attributePaths = {"category", "brand", "imagenes"})
    List<Producto> findByGenero(Genero genero);

    // SALE (productos en rebaja)
    @EntityGraph(attributePaths = {"category", "brand", "imagenes"})
    List<Producto> findByDescuentoGreaterThanAndEstado(BigDecimal descuento, Integer estado);
    @EntityGraph(attributePaths = {"category", "brand", "imagenes"})
    List<Producto> findByDescuentoGreaterThan(BigDecimal descuento);

    // Combinaciones para filtros múltiples
    @EntityGraph(attributePaths = {"category", "brand", "imagenes"})
    List<Producto> findByGeneroAndDestacadoTrueAndEstado(Genero genero, Integer estado);
    @EntityGraph(attributePaths = {"category", "brand", "imagenes"})
    List<Producto> findByGeneroAndDescuentoGreaterThanAndEstado(Genero genero, BigDecimal descuento, Integer estado);

    // ===================== Búsquedas por relaciones =====================

    @EntityGraph(attributePaths = {"category", "brand", "imagenes"})
    List<Producto> findByCategoryAndEstado(Categoria category, Integer estado);
    @EntityGraph(attributePaths = {"category", "brand", "imagenes"})
    List<Producto> findByCategory_IdAndEstado(Long categoryId, Integer estado);
    long countByCategory(Categoria category);

    @EntityGraph(attributePaths = {"category", "brand", "imagenes"})
    List<Producto> findByBrandAndEstado(Marca brand, Integer estado);
    @EntityGraph(attributePaths = {"category", "brand", "imagenes"})
    List<Producto> findByBrand_IdAndEstado(Long brandId, Integer estado);
    long countByBrand(Marca brand);

    // ===================== Búsquedas por texto =====================

    @EntityGraph(attributePaths = {"category", "brand", "imagenes"})
    List<Producto> findByNombreContainingIgnoreCaseAndEstado(String nombre, Integer estado);
    @EntityGraph(attributePaths = {"category", "brand", "imagenes"})
    List<Producto> findByNombreContainingIgnoreCase(String nombre);
    @EntityGraph(attributePaths = {"category", "brand", "imagenes"})
    List<Producto> findByNombreContainingIgnoreCaseOrDescripcionContainingIgnoreCaseAndEstado(
        String nombre, String descripcion, Integer estado);

    // ===================== Filtros avanzados =====================

    @EntityGraph(attributePaths = {"category", "brand", "imagenes"})
    List<Producto> findByPrecioBetweenAndEstado(BigDecimal precioMin, BigDecimal precioMax, Integer estado);

    @EntityGraph(attributePaths = {"category", "brand", "imagenes"})
    List<Producto> findTop10ByEstadoOrderByFechaCreacionDesc(Integer estado);
    @EntityGraph(attributePaths = {"category", "brand", "imagenes"})
    List<Producto> findTop10ByDestacadoTrueAndEstadoOrderByFechaCreacionDesc(Integer estado);

    // ===================== Gestión de Stock ===================== //

    @EntityGraph(attributePaths = {"category", "brand", "imagenes"})
    List<Producto> findByStockAndEstado(Integer stock, Integer estado);
    @EntityGraph(attributePaths = {"category", "brand", "imagenes"})
    List<Producto> findByStockGreaterThanAndEstado(Integer stock, Integer estado);
    @EntityGraph(attributePaths = {"category", "brand", "imagenes"})
    List<Producto> findByStockLessThanEqualAndEstado(Integer stock, Integer estado);

    @Query("SELECT DISTINCT p FROM Producto p " +
           "LEFT JOIN FETCH p.category " +
           "LEFT JOIN FETCH p.brand " +
           "LEFT JOIN FETCH p.imagenes")
    List<Producto> findAllWithRelations();

    /**
     * Listado paginado del panel (excluye eliminados). Solo trae las relaciones a
     * uno: las imágenes se cargan en lote ({@code @BatchSize} en Producto).
     * {@code q} vacío = sin filtro.
     */
    @EntityGraph(attributePaths = {"category", "brand"})
    @Query(value = "SELECT p FROM Producto p WHERE p.estado <> 2 "
            + "AND LOWER(p.nombre) LIKE LOWER(CONCAT('%', :q, '%'))",
            countQuery = "SELECT COUNT(p) FROM Producto p WHERE p.estado <> 2 "
            + "AND LOWER(p.nombre) LIKE LOWER(CONCAT('%', :q, '%'))")
    Page<Producto> buscarPagina(@Param("q") String q, Pageable pageable);
}
