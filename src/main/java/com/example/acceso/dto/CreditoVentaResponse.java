package com.example.acceso.dto;

import com.example.acceso.model.EnumVentas.EstadoCredito;
import com.example.acceso.model.EnumVentas.IntervaloCredito;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditoVentaResponse {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("venta_id")
    private Long ventaId;

    @JsonProperty("comprobante_completo")
    private String comprobanteCompleto;

    @JsonProperty("cliente_id")
    private Long clienteId;

    @JsonProperty("cliente_nombre")
    private String clienteNombre;

    @JsonProperty("cliente_documento")
    private String clienteDocumento;

    @JsonProperty("monto_total")
    private BigDecimal montoTotal;

    @JsonProperty("interes_porcentaje")
    private BigDecimal interesPorcentaje;

    @JsonProperty("monto_con_interes")
    private BigDecimal montoConInteres;

    @JsonProperty("numero_cuotas")
    private Integer numeroCuotas;

    @JsonProperty("monto_inicial")
    private BigDecimal montoInicial;

    @JsonProperty("intervalo_cuotas")
    private IntervaloCredito intervaloCuotas;

    @JsonProperty("fecha_inicio")
    private LocalDate fechaInicio;

    @JsonProperty("fecha_fin")
    private LocalDate fechaFin;

    @JsonProperty("monto_pagado")
    private BigDecimal montoPagado;

    @JsonProperty("saldo_pendiente")
    private BigDecimal saldoPendiente;

    @JsonProperty("tipo_comprobante")
    private String tipoComprobante;

    @JsonProperty("serie")
    private String serie;

    @JsonProperty("numero")
    private String numero;

    @JsonProperty("estado")
    private EstadoCredito estado;

    @JsonProperty("fecha_creacion")
    private LocalDateTime fechaCreacion;

    @JsonProperty("fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @JsonProperty("cuotas")
    private List<CuotaPagoResponse> cuotas;

    @JsonProperty("porcentaje_pagado")
    private BigDecimal porcentajePagado;

    @JsonProperty("dias_restantes")
    private Long diasRestantes;

    public BigDecimal calcularPorcentajePagado() {
        if (montoConInteres.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return montoPagado.multiply(BigDecimal.valueOf(100))
                .divide(montoConInteres, 2, java.math.RoundingMode.HALF_UP);
    }

    public Long calcularDiasRestantes() {
        return java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), fechaFin);
    }
}
