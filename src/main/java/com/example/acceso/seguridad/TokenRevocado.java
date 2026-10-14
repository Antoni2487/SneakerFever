package com.example.acceso.seguridad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Token JWT invalidado antes de su expiración (logout). Se guarda solo el
 * {@code jti} del token, nunca el token completo. La fila deja de servir
 * cuando el token habría expirado de todos modos, y la limpia
 * {@link RevocacionTokenService#limpiarExpirados()}.
 */
@Entity
@Table(name = "tokens_revocados", indexes = @Index(name = "idx_tokens_revocados_expira", columnList = "expira_en"))
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TokenRevocado {

    @Id
    @Column(length = 36)
    private String jti;

    @Column(name = "expira_en", nullable = false)
    private Instant expiraEn;
}
