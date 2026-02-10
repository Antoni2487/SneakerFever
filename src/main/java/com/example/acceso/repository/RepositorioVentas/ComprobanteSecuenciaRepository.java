package com.example.acceso.repository.RepositorioVentas;

import com.example.acceso.model.EntidadesVenta.ComprobanteSecuencia;
import com.example.acceso.model.EnumVentas.TipoComprobante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface ComprobanteSecuenciaRepository extends JpaRepository<ComprobanteSecuencia, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM ComprobanteSecuencia c " +
           "WHERE c.tipoComprobante = :tipo AND c.serie = :serie AND c.activo = true")
    Optional<ComprobanteSecuencia> findByTipoComprobanteAndSerieWithLock(
        @Param("tipo") TipoComprobante tipo,
        @Param("serie") String serie
    );

    // Buscar una secuencia simple sin bloqueo
    Optional<ComprobanteSecuencia> findByTipoComprobanteAndSerie(
        TipoComprobante tipoComprobante,
        String serie
    );
}
