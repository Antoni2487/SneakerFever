package com.example.acceso.dto;

import com.example.acceso.model.EnumVentas.MetodoPago;
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
public class RegistroPagoResponse {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("credito_id")
    private Long creditoId;

    @JsonProperty("cuota_id")
    private Long cuotaId;

    @JsonProperty("numero_cuota")
    private Integer numeroCuota;

    @JsonProperty("monto_pagado")
    private BigDecimal montoPagado;

    @JsonProperty("metodo_pago")  // 👈 AGREGAR ESTE
    private MetodoPago metodoPago;  // 👈 AGREGAR ESTE

    @JsonProperty("numero_operacion")
    private String numeroOperacion;

    @JsonProperty("observaciones")
    private String observaciones;

    @JsonProperty("usuario_registro_pago")
    private String usuarioRegistroPago;

    @JsonProperty("fecha_pago")
    private LocalDateTime fechaPago;
}
