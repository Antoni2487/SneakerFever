package com.example.acceso.dto;

import com.example.acceso.model.EnumVentas.EstadoCuota;
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
public class CuotaPagoResponse {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("credito_id")
    private Long creditoId;

    @JsonProperty("numero_cuota")
    private Integer numeroCuota;

    @JsonProperty("monto_cuota")
    private BigDecimal montoCuota;

    @JsonProperty("fecha_vencimiento")
    private LocalDate fechaVencimiento;

    @JsonProperty("monto_pagado")
    private BigDecimal montoPagado;

    @JsonProperty("saldo_pendiente")
    private BigDecimal saldoPendiente;

    @JsonProperty("fecha_pago")
    private LocalDate fechaPago;

    @JsonProperty("estado")
    private EstadoCuota estado;

    @JsonProperty("dias_para_vencer")
    private Long diasParaVencer;

    @JsonProperty("dias_vencida")
    private Long diasVencida;

    @JsonProperty("porcentaje_pagado")
    private BigDecimal porcentajePagado;

    public Long calcularDiasParaVencer() {
        if (estado == EstadoCuota.PAGADA) {
            return 0L;
        }
        long dias = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), fechaVencimiento);
        return dias > 0 ? dias : 0L;
    }

    public Long calcularDiasVencida() {
        if (estado == EstadoCuota.PAGADA || LocalDate.now().isBefore(fechaVencimiento)) {
            return 0L;
        }
        return java.time.temporal.ChronoUnit.DAYS.between(fechaVencimiento, LocalDate.now());
    }

    public BigDecimal calcularPorcentajePagado() {
        if (montoCuota.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return montoPagado.multiply(BigDecimal.valueOf(100))
                .divide(montoCuota, 2, java.math.RoundingMode.HALF_UP);
    }
}
