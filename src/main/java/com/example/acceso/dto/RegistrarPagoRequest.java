package com.example.acceso.dto;


import com.example.acceso.model.EnumVentas.MetodoPago;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RegistrarPagoRequest {

    @NotNull(message = "El ID del crédito es obligatorio")
    @JsonProperty("credito_id")
    private Long creditoId;

    @NotNull(message = "El ID de la cuota es obligatorio")
    @JsonProperty("cuota_id")
    private Long cuotaId;

    @NotNull(message = "El método de pago es obligatorio")
    @JsonProperty("metodo_pago")
    private MetodoPago metodoPago;

    @NotNull(message = "El monto pagado es obligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "El monto debe ser mayor a 0")
    @JsonProperty("monto_pagado")
    private BigDecimal montoPagado;

    @Size(max = 100, message = "El número de operación no puede exceder 100 caracteres")
    @JsonProperty("numero_operacion")
    private String numeroOperacion;

    @Size(max = 300, message = "Las observaciones no pueden exceder 300 caracteres")
    @JsonProperty("observaciones")
    private String observaciones;
}
