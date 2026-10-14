package com.example.acceso.seguridad;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface TokenRevocadoRepository extends JpaRepository<TokenRevocado, String> {

    @Modifying
    @Query("DELETE FROM TokenRevocado t WHERE t.expiraEn < :ahora")
    int eliminarExpirados(@Param("ahora") Instant ahora);
}
