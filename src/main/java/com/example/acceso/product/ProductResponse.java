package com.example.acceso.product;

import com.example.acceso.brand.BrandResponse;
import com.example.acceso.category.CategoryResponse;
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
public class ProductResponse {

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
    private CategoryResponse category;
    private BrandResponse brand;
    private Integer estado;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;

    public static ProductResponse from(Product producto) {
        return ProductResponse.builder()
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
                .category(producto.getCategory() != null ? CategoryResponse.from(producto.getCategory()) : null)
                .brand(producto.getBrand() != null ? BrandResponse.from(producto.getBrand()) : null)
                .estado(producto.getEstado())
                .fechaCreacion(producto.getFechaCreacion())
                .fechaActualizacion(producto.getFechaActualizacion())
                .build();
    }
}
