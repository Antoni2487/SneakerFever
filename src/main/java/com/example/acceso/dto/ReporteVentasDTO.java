package com.example.acceso.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReporteVentasDTO {

    @JsonProperty("fecha_inicio")
    private LocalDateTime fechaInicio;

    @JsonProperty("fecha_fin")
    private LocalDateTime fechaFin;

    @JsonProperty("total_ventas")
    private Long totalVentas;

    @JsonProperty("ventas_contado")
    private Long ventasContado;

    @JsonProperty("ventas_credito")
    private Long ventasCredito;

    @JsonProperty("monto_total_ventas")
    private BigDecimal montoTotalVentas;

    @JsonProperty("monto_ventas_contado")
    private BigDecimal montoVentasContado;

    @JsonProperty("monto_ventas_credito")
    private BigDecimal montoVentasCredito;

    @JsonProperty("ventas_anuladas")
    private Long ventasAnuladas;

    @JsonProperty("promedio_venta")
    private BigDecimal promedioVenta;

    public BigDecimal calcularPromedioVenta() {
        if (totalVentas == null || totalVentas == 0) {
            return BigDecimal.ZERO;
        }
        return montoTotalVentas.divide(
            BigDecimal.valueOf(totalVentas),
            2,
            java.math.RoundingMode.HALF_UP
        );
    }
}
