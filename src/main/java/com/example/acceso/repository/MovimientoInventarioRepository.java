package com.example.acceso.repository;

import com.example.acceso.model.MovimientoInventario;
import com.example.acceso.model.EnumInventario.TipoMovimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Long> {

    // Listar por producto
    List<MovimientoInventario> findByProductoIdOrderByFechaMovimientoDesc(Long productoId);

    // Listar por tipo de movimiento
    List<MovimientoInventario> findByTipoMovimientoOrderByFechaMovimientoDesc(TipoMovimiento tipo);

    // Listar por rango de fechas
    @Query("SELECT m FROM MovimientoInventario m WHERE m.fechaMovimiento BETWEEN :fechaInicio AND :fechaFin ORDER BY m.fechaMovimiento DESC")
    List<MovimientoInventario> findByFechaMovimientoBetween(
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin
    );

    // Listar por usuario
    List<MovimientoInventario> findByUsuarioIdOrderByFechaMovimientoDesc(Long usuarioId);

    // Últimos movimientos
    List<MovimientoInventario> findTop10ByOrderByFechaMovimientoDesc();

    // Buscar por referencia
    List<MovimientoInventario> findByReferenciaIdAndReferenciaTipo(Long referenciaId, com.example.acceso.model.EnumInventario.TipoReferencia referenciaTipo);
}