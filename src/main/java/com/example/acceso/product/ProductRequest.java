package com.example.acceso.product;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductRequest {

    private Long id;

    @NotBlank(message = "El nombre del producto es obligatorio")
    @Size(min = 2, max = 200, message = "El nombre debe tener entre 2 y 200 caracteres")
    private String nombre;

    @Size(max = 1000, message = "La descripción no puede exceder los 1000 caracteres")
    private String descripcion;

    @Size(max = 500, message = "La URL de la imagen no puede exceder los 500 caracteres")
    private String imagen;

    private List<String> imagenes;

    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "El precio debe ser mayor a 0")
    private BigDecimal precio;

    @DecimalMin(value = "0.0", message = "El descuento no puede ser negativo")
    @DecimalMax(value = "100.0", message = "El descuento no puede ser mayor a 100%")
    private BigDecimal descuento;

    private Boolean destacado;

    @NotNull(message = "El stock es obligatorio")
    @Min(value = 0, message = "El stock no puede ser negativo")
    private Integer stock;

    @NotNull(message = "El stock mínimo es obligatorio")
    @Min(value = 0, message = "El stock mínimo no puede ser negativo")
    private Integer stockMinimo;

    @NotNull(message = "El género es obligatorio")
    private Genero genero;

    @NotNull(message = "La categoría es obligatoria")
    private Long categoryId;

    @NotNull(message = "La marca es obligatoria")
    private Long brandId;

    private Integer estado;

    public Product toEntity() {
        return Product.builder()
                .nombre(nombre)
                .descripcion(descripcion)
                .imagen(imagen)
                .imagenes(imagenes)
                .precio(precio)
                .descuento(descuento)
                .destacado(destacado)
                .stock(stock)
                .stockMinimo(stockMinimo)
                .genero(genero)
                .build();
    }
}
