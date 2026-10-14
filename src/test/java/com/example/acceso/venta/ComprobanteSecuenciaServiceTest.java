package com.example.acceso.venta;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ComprobanteSecuenciaService - numeración de comprobantes")
class ComprobanteSecuenciaServiceTest {

    @Mock
    private ComprobanteSecuenciaRepository repository;

    @InjectMocks
    private ComprobanteSecuenciaService service;

    private ComprobanteSecuencia secuencia(String serie, long numeroActual) {
        ComprobanteSecuencia s = new ComprobanteSecuencia();
        s.setTipoComprobante(TipoComprobante.BOLETA);
        s.setSerie(serie);
        s.setNumeroActual(numeroActual);
        s.setActivo(true);
        return s;
    }

    @Test
    void generarNumero_incrementaYRellenaA8Digitos() {
        ComprobanteSecuencia s = secuencia("B001", 24L);
        when(repository.findByTipoComprobanteAndSerieWithLock(TipoComprobante.BOLETA, "B001")).thenReturn(Optional.of(s));

        String[] resultado = service.generarNumeroComprobante(TipoComprobante.BOLETA, "B001");

        assertThat(resultado).containsExactly("B001", "00000025");
        assertThat(s.getNumeroActual()).isEqualTo(25L);
        verify(repository).save(s);
    }

    @Test
    void generarNumero_sinSecuencia_lanzaExcepcionYNoGuarda() {
        when(repository.findByTipoComprobanteAndSerieWithLock(TipoComprobante.FACTURA, "F009")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generarNumeroComprobante(TipoComprobante.FACTURA, "F009"))
                .hasMessageContaining("No existe una secuencia activa");
        verify(repository, never()).save(any());
    }

    @Test
    void visualizarSiguiente_noModificaElContador() {
        ComprobanteSecuencia s = secuencia("B001", 7L);
        when(repository.findByTipoComprobanteAndSerie(TipoComprobante.BOLETA, "B001")).thenReturn(Optional.of(s));

        assertThat(service.visualizarSiguienteNumero(TipoComprobante.BOLETA, "B001")).isEqualTo("00000008");
        assertThat(s.getNumeroActual()).isEqualTo(7L);
        verify(repository, never()).save(any());
    }
}
