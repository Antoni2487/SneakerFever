package com.example.acceso.venta;

import com.example.acceso.venta.EstadoCuota;
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
}
