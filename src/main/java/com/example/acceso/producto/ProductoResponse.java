package com.example.acceso.producto;

import com.example.acceso.marca.MarcaResponse;
import com.example.acceso.categoria.CategoriaResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductoResponse {

    private Long id;
    private String nombre;
    private String descripcion;
    private String imagen;
    private List<String> imagenes;
    private BigDecimal precio;
    private BigDecimal descuento;
    private Boolean destacado;
    private Integer stock;
    private Integer stockMinimo;
    private Genero genero;
    private CategoriaResponse category;
    private MarcaResponse brand;
    private Integer estado;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;

    public static ProductoResponse from(Producto producto) {
        return ProductoResponse.builder()
                .id(producto.getId())
                .nombre(producto.getNombre())
                .descripcion(producto.getDescripcion())
                .imagen(producto.getImagen())
                .imagenes(producto.getImagenes())
                .precio(producto.getPrecio())
                .descuento(producto.getDescuento())
                .destacado(producto.getDestacado())
                .stock(producto.getStock())
                .stockMinimo(producto.getStockMinimo())
                .genero(producto.getGenero())
                .category(producto.getCategory() != null ? CategoriaResponse.from(producto.getCategory()) : null)
                .brand(producto.getBrand() != null ? MarcaResponse.from(producto.getBrand()) : null)
                .estado(producto.getEstado())
                .fechaCreacion(producto.getFechaCreacion())
                .fechaActualizacion(producto.getFechaActualizacion())
                .build();
    }
}
