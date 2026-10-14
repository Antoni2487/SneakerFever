package com.example.acceso.common;

import com.example.acceso.producto.ProductoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.UnexpectedRollbackException;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ApiResponses.mensajeSeguro - qué mensajes de excepción llegan al cliente")
class ApiResponsesTest {

    @Test
    @DisplayName("Excepción de negocio (RuntimeException/IllegalState/propias): se muestra el mensaje")
    void excepcionDeNegocio_muestraMensaje() {
        assertThat(ApiResponses.mensajeSeguro(new RuntimeException("La venta ya está anulada")))
                .isEqualTo("La venta ya está anulada");
        assertThat(ApiResponses.mensajeSeguro(new IllegalStateException("Stock insuficiente")))
                .isEqualTo("Stock insuficiente");
        assertThat(ApiResponses.mensajeSeguro(new ProductoService.ProductoException("Nombre duplicado")))
                .isEqualTo("Nombre duplicado");
    }

    @Test
    @DisplayName("Excepción técnica (BD, transacción): mensaje genérico")
    void excepcionTecnica_mensajeGenerico() {
        assertThat(ApiResponses.mensajeSeguro(new DataIntegrityViolationException("Duplicate entry 'x' for key 'UK_nombre'")))
                .isEqualTo(ApiResponses.MENSAJE_ERROR_INTERNO);
        assertThat(ApiResponses.mensajeSeguro(new UnexpectedRollbackException("Transaction silently rolled back")))
                .isEqualTo(ApiResponses.MENSAJE_ERROR_INTERNO);
        assertThat(ApiResponses.mensajeSeguro(new NullPointerException("Cannot invoke ...")))
                .isEqualTo(ApiResponses.MENSAJE_ERROR_INTERNO);
    }

    @Test
    @DisplayName("Excepción de negocio que envuelve una técnica: mensaje genérico")
    void negocioEnvolviendoTecnica_mensajeGenerico() {
        RuntimeException envuelta = new ProductoService.ProductoException(
                "Error al guardar el producto: Duplicate entry 'x'",
                new DataIntegrityViolationException("Duplicate entry 'x'"));
        assertThat(ApiResponses.mensajeSeguro(envuelta)).isEqualTo(ApiResponses.MENSAJE_ERROR_INTERNO);
    }

    @Test
    @DisplayName("errorDeNegocio: 400 para reglas de negocio, 500 para errores técnicos")
    void errorDeNegocio_eligeElCodigoHttp() {
        assertThat(ApiResponses.errorDeNegocio(new IllegalStateException("Stock insuficiente")).getStatusCode().value())
                .isEqualTo(400);
        assertThat(ApiResponses.errorDeNegocio(new DataIntegrityViolationException("Duplicate entry")).getStatusCode().value())
                .isEqualTo(500);
    }
}
