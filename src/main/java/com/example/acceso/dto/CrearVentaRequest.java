package com.example.acceso.dto;

import com.example.acceso.model.EnumVentas.FormaPago;
import com.example.acceso.model.EnumVentas.IntervaloCredito;
import com.example.acceso.model.EnumVentas.TipoComprobante;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CrearVentaRequest {

    @JsonProperty("documento")
    private String documento;

    @JsonProperty("tipo_documento")
    private String tipoDocumento;

    @JsonProperty("cliente_id")
    private Long clienteId;

    @NotNull(message = "El tipo de comprobante es obligatorio")
    @JsonProperty("tipo_comprobante")
    private TipoComprobante tipoComprobante;

    @NotBlank(message = "La serie es obligatoria")
    @JsonProperty("serie")
    private String serie;

    @NotNull(message = "La forma de pago es obligatoria")
    @JsonProperty("forma_pago")
    private FormaPago formaPago;

    @DecimalMin(value = "0.0", message = "El descuento general no puede ser negativo")
    @DecimalMax(value = "100.0", message = "El descuento general no puede exceder 100%")
    @JsonProperty("descuento_general")
    private BigDecimal descuentoGeneral = BigDecimal.ZERO;

    @Size(max = 500, message = "Las observaciones no pueden exceder 500 caracteres")
    @JsonProperty("observaciones")
    private String observaciones;

    @NotEmpty(message = "Debe incluir al menos un detalle de venta")
    @Valid
    @JsonProperty("detalles")
    private List<DetalleVentaRequest> detalles;

    @Valid
    @JsonProperty("credito")
    private CreditoVentaRequest credito;


    @Data
    public static class DetalleVentaRequest {

        @NotNull(message = "El ID del producto es obligatorio")
        @JsonProperty("producto_id")
        private Long productoId;

        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad debe ser al menos 1")
        @JsonProperty("cantidad")
        private Integer cantidad;

        @NotNull(message = "El precio unitario es obligatorio")
        @DecimalMin(value = "0.0", inclusive = false, message = "El precio unitario debe ser mayor a 0")
        @JsonProperty("precio_unitario")
        private BigDecimal precioUnitario;

        @DecimalMin(value = "0.0", message = "El descuento no puede ser negativo")
        @DecimalMax(value = "100.0", message = "El descuento no puede exceder 100%")
        @JsonProperty("descuento_porcentaje")
        private BigDecimal descuentoPorcentaje = BigDecimal.ZERO;


    }

    @Data
        public static class CreditoVentaRequest {

        @NotNull(message = "El número de cuotas es obligatorio")
        @Min(value = 1, message = "El número de cuotas debe ser al menos 1")
        @JsonProperty("numero_cuotas")
        private Integer numeroCuotas;

        @NotNull(message = "El intervalo de cuotas es obligatorio")
        @JsonProperty("intervalo_cuotas")
        private IntervaloCredito intervaloCuotas;

        @DecimalMin(value = "0.0", message = "El interés no puede ser negativo")
        @DecimalMax(value = "100.0", message = "El interés no puede exceder 100%")
        @JsonProperty("interes_porcentaje")
        private BigDecimal interesPorcentaje = BigDecimal.ZERO;

        @DecimalMin(value = "0.0", message = "La inicial no puede ser negativa")
        @JsonProperty("monto_inicial")
        private BigDecimal montoInicial = BigDecimal.ZERO;


    }


}
