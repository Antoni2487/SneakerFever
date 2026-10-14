package com.example.acceso.seguridad;

import com.example.acceso.usuario.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JwtService - emisión y validación de tokens")
class JwtServiceTest {

    private static final String SECRET = "secreto-de-prueba-0123456789abcdefghijklmnop";

    private JwtService jwtService;
    private final Usuario usuario = Usuario.builder().id(5L).usuario("vendedor").build();

    private JwtService crear(String secret, long accessMinutos) {
        JwtService s = new JwtService();
        ReflectionTestUtils.setField(s, "secret", secret);
        ReflectionTestUtils.setField(s, "accessTokenExpirationMinutes", accessMinutos);
        ReflectionTestUtils.setField(s, "refreshTokenExpirationDays", 7L);
        return s;
    }

    @BeforeEach
    void setUp() {
        jwtService = crear(SECRET, 30);
    }

    @Test
    void accessToken_valido_traeUsuarioYTipo() {
        Claims claims = jwtService.parseAndValidate(jwtService.generateAccessToken(usuario), JwtService.TOKEN_TYPE_ACCESS);

        assertThat(claims.getSubject()).isEqualTo("vendedor");
        assertThat(claims.get("userId", Long.class)).isEqualTo(5L);
    }

    @Test
    void tokenDelTipoEquivocado_seRechaza() {
        String refresh = jwtService.generateRefreshToken(usuario);
        String access = jwtService.generateAccessToken(usuario);

        assertThatThrownBy(() -> jwtService.parseAndValidate(refresh, JwtService.TOKEN_TYPE_ACCESS))
                .isInstanceOf(JwtException.class);
        assertThatThrownBy(() -> jwtService.parseAndValidate(access, JwtService.TOKEN_TYPE_REFRESH))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void tokenFirmadoConOtroSecreto_seRechaza() {
        String ajeno = crear("otro-secreto-distinto-0123456789abcdefghijk", 30).generateAccessToken(usuario);

        assertThatThrownBy(() -> jwtService.parseAndValidate(ajeno, JwtService.TOKEN_TYPE_ACCESS))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void tokenExpirado_seRechaza() {
        String expirado = crear(SECRET, -1).generateAccessToken(usuario);

        assertThatThrownBy(() -> jwtService.parseAndValidate(expirado, JwtService.TOKEN_TYPE_ACCESS))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void refreshCookieMaxAge_esDe7Dias() {
        assertThat(jwtService.getRefreshTokenExpirationSeconds()).isEqualTo(7L * 24 * 3600);
    }

    @Test
    void secretoCortoOVacio_fallaAlArrancar() {
        assertThatThrownBy(() -> crear("corto", 30).validarSecreto())
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("32 caracteres");
        assertThatThrownBy(() -> crear("", 30).validarSecreto()).isInstanceOf(IllegalStateException.class);
    }
}
