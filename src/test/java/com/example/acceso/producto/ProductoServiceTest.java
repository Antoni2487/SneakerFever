package com.example.acceso.producto;

import com.example.acceso.categoria.Categoria;
import com.example.acceso.categoria.CategoriaRepository;
import com.example.acceso.marca.Marca;
import com.example.acceso.marca.MarcaRepository;
import com.example.acceso.producto.ProductoService.ProductoException;
import com.example.acceso.venta.DetalleVentaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductoService - validaciones y ciclo de vida del producto")
class ProductoServiceTest {

    @Mock
    private ProductoRepository productRepository;

    @Mock
    private DetalleVentaRepository detalleVentaRepository;

    @Mock
    private CategoriaRepository categoryRepository;

    @Mock
    private MarcaRepository brandRepository;

    @InjectMocks
    private ProductoService productoService;

    private final Categoria categoria = Categoria.builder().id(1L).nombre("Zapatillas").build();
    private final Marca marca = Marca.builder().id(1L).nombre("Nike").build();

    private ProductoRequest request() {
        ProductoRequest r = new ProductoRequest();
        r.setNombre("  Air Max  ");
        r.setPrecio(new BigDecimal("399.90"));
        r.setStock(10);
        r.setStockMinimo(2);
        r.setGenero(Genero.HOMBRE);
        r.setCategoryId(1L);
        r.setBrandId(1L);
        return r;
    }

    private void mockCategoriaYMarca() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(brandRepository.findById(1L)).thenReturn(Optional.of(marca));
    }

    @Test
    void crear_normalizaNombreYAplicaValoresPorDefecto() {
        mockCategoriaYMarca();
        when(productRepository.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));
        ProductoRequest r = request();
        r.setDescuento(BigDecimal.ZERO);
        r.setImagenes(List.of("/uploads/productos/a.png", "/uploads/productos/b.png"));

        Producto p = productoService.guardarProducto(r);

        assertThat(p.getNombre()).isEqualTo("Air Max");
        assertThat(p.getEstado()).isEqualTo(1);
        assertThat(p.getDestacado()).isFalse();
        assertThat(p.getDescuento()).as("descuento 0 se guarda como sin descuento").isNull();
        assertThat(p.getImagen()).as("la primera imagen es la principal").isEqualTo("/uploads/productos/a.png");
        assertThat(p.getCategory()).isEqualTo(categoria);
    }

    @Test
    void precioCeroODescuentoFueraDeRango_seRechazanSinGuardar() {
        mockCategoriaYMarca();
        ProductoRequest sinPrecio = request();
        sinPrecio.setPrecio(BigDecimal.ZERO);
        assertThatThrownBy(() -> productoService.guardarProducto(sinPrecio))
                .isInstanceOf(ProductoException.class).hasMessageContaining("mayor a 0");

        ProductoRequest descuentoMalo = request();
        descuentoMalo.setDescuento(new BigDecimal("150"));
        assertThatThrownBy(() -> productoService.guardarProducto(descuentoMalo))
                .isInstanceOf(ProductoException.class).hasMessageContaining("entre 0 y 100");

        verify(productRepository, never()).save(any());
    }

    @Test
    void categoriaInexistente_seRechaza() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productoService.guardarProducto(request()))
                .isInstanceOf(ProductoException.class).hasMessageContaining("Categoría no encontrada");
    }

    @Test
    void eliminar_esBorradoLogicoConEstado2() {
        Producto p = Producto.builder().id(5L).nombre("X").estado(1).build();
        when(productRepository.findById(5L)).thenReturn(Optional.of(p));

        productoService.eliminarProducto(5L);

        assertThat(p.getEstado()).isEqualTo(2);
        verify(productRepository).save(p);
    }

    @Test
    void cambiarEstado_alternaActivoInactivo() {
        Producto p = Producto.builder().id(5L).nombre("X").estado(1).build();
        when(productRepository.findById(5L)).thenReturn(Optional.of(p));
        when(productRepository.save(p)).thenReturn(p);

        assertThat(productoService.cambiarEstadoProducto(5L)).get().extracting(Producto::getEstado).isEqualTo(0);
    }

    @Test
    void actualizarStockNegativo_seIgnora() {
        assertThat(productoService.actualizarStock(5L, -1)).isEmpty();
        verify(productRepository, never()).save(any());
    }
}
