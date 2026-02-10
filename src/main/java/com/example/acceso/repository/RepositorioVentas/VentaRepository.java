package com.example.acceso.repository.RepositorioVentas;

import com.example.acceso.model.EnumVentas.EstadoVenta;
import com.example.acceso.model.EntidadesVenta.Venta;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VentaRepository extends JpaRepository<Venta, Long> {


    // ===================== BÚSQUEDAS POR CLIENTE =====================

    List<Venta> findByClienteIdOrderByFechaCreacionDesc(Long clienteId);

    // ===================== BÚSQUEDAS POR ESTADO =====================

    List<Venta> findByEstadoOrderByFechaCreacionDesc(EstadoVenta estado);

    Long countByEstadoNot(EstadoVenta estado);


    // ===================== BÚSQUEDAS POR FORMA DE PAGO =====================

    @Query("SELECT v FROM Venta v WHERE v.formaPago = 'CREDITO' " +
           "ORDER BY v.fechaCreacion DESC")
    List<Venta> findVentasConCredito();

    // ===================== BÚSQUEDAS POR FECHA =====================

    List<Venta> findByFechaCreacionBetweenOrderByFechaCreacionDesc(
        LocalDateTime fechaInicio,
        LocalDateTime fechaFin
    );

    @Query("SELECT COALESCE(SUM(v.total), 0) FROM Venta v " +
           "WHERE v.fechaCreacion BETWEEN :fechaInicio AND :fechaFin " +
           "AND v.estado != 'ANULADA'")
    BigDecimal sumTotalByFechaCreacionBetween(
        @Param("fechaInicio") LocalDateTime fechaInicio,
        @Param("fechaFin") LocalDateTime fechaFin
    );

    // ===================== ESTADÍSTICAS PARA DASHBOARD =====================

    /**
     * Sumar total de ventas en un rango de fechas excluyendo ANULADAS
     */
    @Query("SELECT COALESCE(SUM(v.total), 0) FROM Venta v " +
           "WHERE v.fechaCreacion BETWEEN :inicio AND :fin " +
           "AND v.estado <> :estado")
    BigDecimal sumTotalByFechaCreacionBetweenAndEstadoNot(
        @Param("inicio") LocalDateTime inicio,
        @Param("fin") LocalDateTime fin,
        @Param("estado") EstadoVenta estado
    );

    // ===================== CARGA OPTIMIZADA CON RELACIONES =====================

    @EntityGraph(attributePaths = {"cliente", "detalles", "detalles.producto"})
    @Query("SELECT v FROM Venta v WHERE v.id = :id")
    Optional<Venta> findByIdWithDetalles(@Param("id") Long id);

    @EntityGraph(attributePaths = {"cliente", "detalles", "detalles.producto", "credito", "credito.cuotas"})
    @Query("SELECT v FROM Venta v WHERE v.credito IS NOT NULL ORDER BY v.fechaCreacion DESC")
    List<Venta> findVentasConCreditoWithDetails();

    
}