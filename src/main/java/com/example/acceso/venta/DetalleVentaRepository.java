package com.example.acceso.venta;

import com.example.acceso.venta.DetalleVenta;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DetalleVentaRepository extends JpaRepository<DetalleVenta, Long> {

    /**
     * Obtiene los 5 productos más vendidos por cantidad de unidades vendidas
     * Solo cuenta ventas NO ANULADAS
     */
    @Query("SELECT dv.producto, SUM(dv.cantidad) as totalVendido " +
           "FROM DetalleVenta dv " +
           "JOIN dv.venta v " +
           "WHERE v.estado != 'ANULADA' " +
           "AND dv.producto.category.nombre = :categoriaNombre " +
           "AND dv.producto.estado = 1 " +
           "GROUP BY dv.producto " +
           "ORDER BY totalVendido DESC")
    List<Object[]> findTop5ProductosMasVendidosPorCategoria(@Param("categoriaNombre") String categoriaNombre);

    /**
     * Ranking global de productos más vendidos (todas las categorías), para el dashboard.
     * Solo cuenta ventas NO ANULADAS. Cada fila: [productoId, productoNombre, cantidadVendida, totalIngresos]
     */
    @Query("SELECT dv.producto.id, dv.producto.nombre, SUM(dv.cantidad), SUM(dv.subtotal) " +
           "FROM DetalleVenta dv " +
           "JOIN dv.venta v " +
           "WHERE v.estado != 'ANULADA' " +
           "GROUP BY dv.producto.id, dv.producto.nombre " +
           "ORDER BY SUM(dv.cantidad) DESC")
    List<Object[]> findProductosMasVendidos(Pageable pageable);
}