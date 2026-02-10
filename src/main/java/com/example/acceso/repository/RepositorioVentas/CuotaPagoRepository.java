package com.example.acceso.repository.RepositorioVentas;

import com.example.acceso.model.EntidadesVenta.CuotaPago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CuotaPagoRepository extends JpaRepository<CuotaPago, Long> {

    // Buscar cuotas vencidas
    @Query("SELECT c FROM CuotaPago c WHERE c.fechaVencimiento < :fecha " +
           "AND c.estado IN ('PENDIENTE', 'PARCIAL')")
    List<CuotaPago> findCuotasVencidas(@Param("fecha") LocalDate fecha);
}