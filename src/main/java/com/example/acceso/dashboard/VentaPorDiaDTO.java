package com.example.acceso.dashboard;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VentaPorDiaDTO {

    @JsonProperty("fecha")
    private LocalDate fecha;

    @JsonProperty("total")
    private BigDecimal total;

    @JsonProperty("cantidad_ventas")
    private Long cantidadVentas;
}
