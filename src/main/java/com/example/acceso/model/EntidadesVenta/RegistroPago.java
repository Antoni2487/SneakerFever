package com.example.acceso.model.EntidadesVenta;

import com.example.acceso.model.EnumVentas.MetodoPago;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "registros_pago")
public class RegistroPago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credito_id", nullable = false)
    private CreditoVenta credito;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuota_id")
    private CuotaPago cuota;

    @NotNull(message = "El monto pagado es obligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "El monto debe ser mayor a 0")
    @Column(name = "monto_pagado", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoPagado;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pago")
    private MetodoPago metodoPago;

    @Size(max = 100, message = "El número de operación no puede exceder 100 caracteres")
    @Column(name = "numero_operacion", length = 100)
    private String numeroOperacion;

    @Size(max = 300, message = "Las observaciones no pueden exceder 300 caracteres")
    @Column(name = "observaciones", length = 300)
    private String observaciones;

    @NotBlank(message = "El usuario que registra el pago es obligatorio")
    @Size(max = 100, message = "El usuario no puede exceder 100 caracteres")
    @Column(name = "usuario_registro_pago", nullable = false, length = 100)
    private String usuarioRegistroPago;

    @CreationTimestamp
    @Column(name = "fecha_pago", nullable = false, updatable = false)
    private LocalDateTime fechaPago;

    @Override
    public String toString() {
        return "RegistroPago{" +
                "id=" + id +
                ", montoPagado=" + montoPagado +
                ", fechaPago=" + fechaPago +
                ", usuarioRegistroPago='" + usuarioRegistroPago + '\'' +
                '}';
    }
}
