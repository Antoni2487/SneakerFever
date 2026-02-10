package com.example.acceso.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReporteCreditosDTO {

    @JsonProperty("total_creditos")
    private Long totalCreditos;

    @JsonProperty("creditos_activos")
    private Long creditosActivos;

    @JsonProperty("creditos_pagados")
    private Long creditosPagados;

    @JsonProperty("creditos_vencidos")
    private Long creditosVencidos;

    @JsonProperty("monto_total_creditos")
    private BigDecimal montoTotalCreditos;

    @JsonProperty("monto_total_pagado")
    private BigDecimal montoTotalPagado;

    @JsonProperty("saldo_pendiente_total")
    private BigDecimal saldoPendienteTotal;

    @JsonProperty("porcentaje_recuperacion")
    private BigDecimal porcentajeRecuperacion;

    public BigDecimal calcularPorcentajeRecuperacion() {
        if (montoTotalCreditos == null || montoTotalCreditos.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return montoTotalPagado.multiply(BigDecimal.valueOf(100))
                .divide(montoTotalCreditos, 2, java.math.RoundingMode.HALF_UP);
    }
}
