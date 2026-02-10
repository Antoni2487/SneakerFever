package com.example.acceso.repository.RepositorioVentas;

import com.example.acceso.model.EnumVentas.EstadoCredito;
import com.example.acceso.model.EntidadesVenta.CreditoVenta;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CreditoVentaRepository extends JpaRepository<CreditoVenta, Long> {

    // ===================== Búsquedas básicas =====================

    Optional<CreditoVenta> findByVentaId(Long ventaId);

    // ===================== Búsquedas por Cliente =====================

    @Query("SELECT c FROM CreditoVenta c WHERE c.venta.cliente.id = :clienteId " +
           "ORDER BY c.fechaCreacion DESC")
    List<CreditoVenta> findByClienteIdOrderByFechaCreacionDesc(@Param("clienteId") Long clienteId);

    // ===================== Búsquedas por Estado =====================

    List<CreditoVenta> findByEstadoOrderByFechaCreacionDesc(EstadoCredito estado);

    // ===================== Créditos Vencidos =====================

    @Query("SELECT c FROM CreditoVenta c WHERE c.estado = 'ACTIVO' " +
           "AND c.fechaFin < :fecha AND c.saldoPendiente > 0 " +
           "ORDER BY c.fechaFin ASC")
    List<CreditoVenta> findCreditosVencidos(@Param("fecha") LocalDate fecha);

    // ===================== Carga optimizada con relaciones =====================

    @EntityGraph(attributePaths = {"venta", "venta.cliente", "cuotas"})
    @Query("SELECT c FROM CreditoVenta c WHERE c.id = :id")
    Optional<CreditoVenta> findByIdWithCuotas(@Param("id") Long id);
}