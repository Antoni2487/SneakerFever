package com.example.acceso.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductoMasVendidoDTO {

    @JsonProperty("producto_id")
    private Long productoId;

    @JsonProperty("producto_nombre")
    private String productoNombre;

    @JsonProperty("cantidad_vendida")
    private Long cantidadVendida;

    @JsonProperty("total_ingresos")
    private BigDecimal totalIngresos;
}
