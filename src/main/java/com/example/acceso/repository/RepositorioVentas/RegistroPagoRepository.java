package com.example.acceso.repository.RepositorioVentas;

import com.example.acceso.model.EntidadesVenta.RegistroPago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RegistroPagoRepository extends JpaRepository<RegistroPago, Long> {

    /**
     * Buscar todos los pagos de un crédito específico ordenados por fecha
     */
    List<RegistroPago> findByCreditoIdOrderByFechaPagoDesc(Long creditoId);

    /**
     * Buscar pagos de una cuota específica
     */
    List<RegistroPago> findByCuotaIdOrderByFechaPagoDesc(Long cuotaId);

    /**
     * Buscar el último pago de una cuota
     */
    Optional<RegistroPago> findFirstByCuotaIdOrderByFechaPagoDesc(Long cuotaId);

    /**
     * Contar pagos de un crédito
     */
    long countByCreditoId(Long creditoId);

    /**
     * Buscar pagos por usuario
     */
    List<RegistroPago> findByUsuarioRegistroPagoOrderByFechaPagoDesc(String usuario);
}