package com.example.acceso.model.EntidadesVenta;

import com.example.acceso.model.EnumVentas.EstadoCuota;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "cuotas_pago")
public class CuotaPago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credito_id", nullable = false)
    private CreditoVenta credito;

    @NotNull(message = "El número de cuota es obligatorio")
    @Min(value = 1, message = "El número de cuota debe ser al menos 1")
    @Column(name = "numero_cuota", nullable = false)
    private Integer numeroCuota;

    @NotNull(message = "El monto de la cuota es obligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "El monto debe ser mayor a 0")
    @Column(name = "monto_cuota", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoCuota;

    @NotNull(message = "La fecha de vencimiento es obligatoria")
    @Column(name = "fecha_vencimiento", nullable = false)
    private LocalDate fechaVencimiento;

    @NotNull(message = "El monto pagado es obligatorio")
    @DecimalMin(value = "0.0", message = "El monto pagado no puede ser negativo")
    @Column(name = "monto_pagado", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal montoPagado = BigDecimal.ZERO;

    @NotNull(message = "El saldo pendiente es obligatorio")
    @DecimalMin(value = "0.0", message = "El saldo pendiente no puede ser negativo")
    @Column(name = "saldo_pendiente", nullable = false, precision = 10, scale = 2)
    private BigDecimal saldoPendiente;

    @Column(name = "fecha_pago")
    private LocalDate fechaPago;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    @Builder.Default
    private EstadoCuota estado = EstadoCuota.PENDIENTE;

    @CreationTimestamp
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @UpdateTimestamp
    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    /**
     * Actualiza el estado de la cuota basado en el saldo y la fecha de vencimiento
     */
    public void actualizarEstado() {
        if (saldoPendiente.compareTo(BigDecimal.ZERO) == 0) {
            estado = com.example.acceso.model.EnumVentas.EstadoCuota.PAGADA;
        } else if (montoPagado.compareTo(BigDecimal.ZERO) > 0 && saldoPendiente.compareTo(BigDecimal.ZERO) > 0) {
            estado = com.example.acceso.model.EnumVentas.EstadoCuota.PARCIAL;
        } else if (java.time.LocalDate.now().isAfter(fechaVencimiento) && saldoPendiente.compareTo(BigDecimal.ZERO) > 0) {
            estado = com.example.acceso.model.EnumVentas.EstadoCuota.VENCIDA;
        } else {
            estado = com.example.acceso.model.EnumVentas.EstadoCuota.PENDIENTE;
        }
    }

    /**
     * Registra un pago sobre esta cuota
     */
    public void registrarPago(BigDecimal monto) {
        if (monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto del pago debe ser mayor a 0");
        }
        if (monto.compareTo(saldoPendiente) > 0) {
            throw new IllegalArgumentException("El monto del pago no puede ser mayor al saldo pendiente de la cuota");
        }
        this.montoPagado = this.montoPagado.add(monto);
        this.saldoPendiente = this.saldoPendiente.subtract(monto);
        if (this.saldoPendiente.compareTo(BigDecimal.ZERO) == 0) {
            this.fechaPago = java.time.LocalDate.now();
        }
        actualizarEstado();
    }

    @Override
    public String toString() {
        return "CuotaPago{" +
                "id=" + id +
                ", numeroCuota=" + numeroCuota +
                ", montoCuota=" + montoCuota +
                ", fechaVencimiento=" + fechaVencimiento +
                ", montoPagado=" + montoPagado +
                ", saldoPendiente=" + saldoPendiente +
                ", estado=" + estado +
                '}';
    }
}
