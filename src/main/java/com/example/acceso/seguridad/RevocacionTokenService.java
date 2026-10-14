package com.example.acceso.seguridad;

import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Lista de tokens revocados (logout del lado del servidor). Un JWT es válido
 * por firma hasta que expira; para cortarlo antes, su {@code jti} queda
 * registrado aquí y el filtro y /api/auth/refresh lo rechazan.
 *
 * Es por token, no por usuario: cerrar sesión en un dispositivo no cierra las
 * de los demás.
 */
@Service
public class RevocacionTokenService {

    private static final Logger log = LoggerFactory.getLogger(RevocacionTokenService.class);

    private final TokenRevocadoRepository repository;

    public RevocacionTokenService(TokenRevocadoRepository repository) {
        this.repository = repository;
    }

    /** Revoca el token de estos claims (ya validados). Idempotente. */
    @Transactional
    public void revocar(Claims claims) {
        String jti = claims.getId();
        if (jti == null || claims.getExpiration() == null || repository.existsById(jti)) {
            return;
        }
        repository.save(new TokenRevocado(jti, claims.getExpiration().toInstant()));
    }

    @Transactional(readOnly = true)
    public boolean estaRevocado(String jti) {
        return jti != null && repository.existsById(jti);
    }

    /** Cada hora: un token revocado que ya expiró no necesita seguir en la tabla. */
    @Scheduled(fixedRateString = "PT1H", initialDelayString = "PT1M")
    @Transactional
    public void limpiarExpirados() {
        int eliminados = repository.eliminarExpirados(Instant.now());
        if (eliminados > 0) {
            log.info("Tokens revocados expirados eliminados: {}", eliminados);
        }
    }
}
