package com.example.acceso.inventario;

import com.example.acceso.producto.Producto;
import com.example.acceso.producto.ProductoRepository;
import com.example.acceso.usuario.Usuario;
import com.example.acceso.usuario.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MovimientoInventarioService - tests unitarios con Mockito")
class MovimientoInventarioServiceTest {

    @Mock
    private MovimientoInventarioRepository movimientoRepository;

    @Mock
    private ProductoRepository productRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private MovimientoInventarioService movimientoService;

    private Producto producto(int stock) {
        return Producto.builder().id(10L).nombre("Zapatilla X").precio(new BigDecimal("100.00")).stock(stock).estado(1).build();
    }

    private RegistrarMovimientoRequest salida(int cantidad) {
        return RegistrarMovimientoRequest.builder()
                .productoId(10L)
                .tipoMovimiento(TipoMovimiento.SALIDA)
                .cantidad(cantidad)
                .motivo(MotivoMovimiento.VENTA)
                .build();
    }

    private void mockUsuarioYGuardado() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(Usuario.builder().id(1L).usuario("admin").build()));
        when(movimientoRepository.save(any(MovimientoInventario.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("registrarMovimiento SALIDA: descuenta stock y guarda anterior/nuevo")
    void registrarMovimientoSalida_descuentaStock() {
        Producto producto = producto(5);
        when(productRepository.findById(10L)).thenReturn(Optional.of(producto));
        mockUsuarioYGuardado();

        movimientoService.registrarMovimiento(salida(2), 1L);

        assertThat(producto.getStock()).isEqualTo(3);
        ArgumentCaptor<MovimientoInventario> captor = ArgumentCaptor.forClass(MovimientoInventario.class);
        verify(movimientoRepository).save(captor.capture());
        assertThat(captor.getValue().getStockAnterior()).isEqualTo(5);
        assertThat(captor.getValue().getStockNuevo()).isEqualTo(3);
    }

    @Test
    @DisplayName("registrarMovimiento SALIDA sin stock suficiente: lanza excepción y no guarda")
    void registrarMovimientoSalidaSinStock_lanzaExcepcion() {
        when(productRepository.findById(10L)).thenReturn(Optional.of(producto(1)));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(Usuario.builder().id(1L).build()));

        assertThatThrownBy(() -> movimientoService.registrarMovimiento(salida(2), 1L))
                .hasMessageContaining("Stock insuficiente");
        verify(movimientoRepository, never()).save(any());
    }

    @Test
    @DisplayName("registrarMovimientoYaAplicado: NO vuelve a tocar el stock y reconstruye el anterior")
    void registrarMovimientoYaAplicado_noTocaStock() {
        // Stock 1 = lo que quedó después de que VentaService descontara 2 de 3.
        Producto producto = producto(1);
        when(productRepository.findById(10L)).thenReturn(Optional.of(producto));
        mockUsuarioYGuardado();

        movimientoService.registrarMovimientoYaAplicado(salida(2), 1L);

        assertThat(producto.getStock()).isEqualTo(1);
        verify(productRepository, never()).save(any());
        ArgumentCaptor<MovimientoInventario> captor = ArgumentCaptor.forClass(MovimientoInventario.class);
        verify(movimientoRepository).save(captor.capture());
        assertThat(captor.getValue().getStockAnterior()).isEqualTo(3);
        assertThat(captor.getValue().getStockNuevo()).isEqualTo(1);
    }
}
