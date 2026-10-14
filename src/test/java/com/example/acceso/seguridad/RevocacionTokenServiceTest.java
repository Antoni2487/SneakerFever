package com.example.acceso.seguridad;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RevocacionTokenService - lista de tokens revocados")
class RevocacionTokenServiceTest {

    @Mock
    private TokenRevocadoRepository repository;

    @InjectMocks
    private RevocacionTokenService service;

    private Claims claims(String jti, Instant expira) {
        return Jwts.claims().id(jti).expiration(Date.from(expira)).build();
    }

    @Test
    void revocar_guardaJtiYExpiracion() {
        Instant expira = Instant.now().plusSeconds(1800);
        when(repository.existsById("abc")).thenReturn(false);

        service.revocar(claims("abc", expira));

        ArgumentCaptor<TokenRevocado> captor = ArgumentCaptor.forClass(TokenRevocado.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getJti()).isEqualTo("abc");
        assertThat(captor.getValue().getExpiraEn()).isEqualTo(expira.truncatedTo(java.time.temporal.ChronoUnit.SECONDS));
    }

    @Test
    void revocarDosVeces_esIdempotente() {
        when(repository.existsById("abc")).thenReturn(true);

        service.revocar(claims("abc", Instant.now().plusSeconds(60)));

        verify(repository, never()).save(any());
    }

    @Test
    void jtiNulo_noEstaRevocadoNiSeGuarda() {
        assertThat(service.estaRevocado(null)).isFalse();
        service.revocar(Jwts.claims().expiration(new Date()).build());
        verify(repository, never()).save(any());
    }

    @Test
    void limpiarExpirados_borraLosVencidos() {
        when(repository.eliminarExpirados(any())).thenReturn(3);

        service.limpiarExpirados();

        verify(repository).eliminarExpirados(any(Instant.class));
    }
}
