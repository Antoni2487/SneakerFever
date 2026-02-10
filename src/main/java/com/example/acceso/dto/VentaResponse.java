package com.example.acceso.dto;

import com.example.acceso.model.EnumVentas.EstadoVenta;
import com.example.acceso.model.EnumVentas.FormaPago;
import com.example.acceso.model.EnumVentas.TipoComprobante;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VentaResponse {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("cliente_id")
    private Long clienteId;

    @JsonProperty("cliente_nombre")
    private String clienteNombre;

    @JsonProperty("cliente_documento")
    private String clienteDocumento;

    @JsonProperty("tipo_comprobante")
    private TipoComprobante tipoComprobante;

    @JsonProperty("serie")
    private String serie;

    @JsonProperty("numero")
    private String numero;

    @JsonProperty("comprobante_completo")
    private String comprobanteCompleto;

    @JsonProperty("forma_pago")
    private FormaPago formaPago;


    @JsonProperty("subtotal")
    private BigDecimal subtotal;

    @JsonProperty("descuento_general")
    private BigDecimal descuentoGeneral;

    @JsonProperty("igv")
    private BigDecimal igv;

    @JsonProperty("total")
    private BigDecimal total;

    @JsonProperty("estado")
    private EstadoVenta estado;

    @JsonProperty("observaciones")
    private String observaciones;

    @JsonProperty("usuario_creacion")
    private String usuarioCreacion;

    @JsonProperty("fecha_creacion")
    private LocalDateTime fechaCreacion;

    @JsonProperty("fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @JsonProperty("detalles")
    private List<DetalleVentaResponse> detalles;

    @JsonProperty("tiene_credito")
    private Boolean tieneCredito;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
    public static class DetalleVentaResponse {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("producto_id")
    private Long productoId;

    @JsonProperty("producto_nombre")
    private String productoNombre;

    @JsonProperty("cantidad")
    private Integer cantidad;

    @JsonProperty("precio_unitario")
    private BigDecimal precioUnitario;

    @JsonProperty("descuento_porcentaje")
    private BigDecimal descuentoPorcentaje;

    @JsonProperty("subtotal")
    private BigDecimal subtotal;

   }

}
