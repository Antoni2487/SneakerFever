package com.example.acceso.model.EntidadesVenta;


import com.example.acceso.model.EnumVentas.EstadoCredito;
import com.example.acceso.model.EnumVentas.IntervaloCredito;
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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "creditos_venta")
public class CreditoVenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venta_id", nullable = false, unique = true)
    private Venta venta;

    @NotNull(message = "El monto total del crédito es obligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "El monto debe ser mayor a 0")
    @Column(name = "monto_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoTotal;

    @DecimalMin(value = "0.0", message = "El interés no puede ser negativo")
    @DecimalMax(value = "100.0", message = "El interés no puede exceder 100%")
    @Column(name = "interes_porcentaje", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal interesPorcentaje = BigDecimal.ZERO;

    @NotNull(message = "El monto con interés es obligatorio")
    @DecimalMin(value = "0.0", message = "El monto con interés no puede ser negativo")
    @Column(name = "monto_con_interes", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoConInteres;

    @NotNull(message = "El número de cuotas es obligatorio")
    @Min(value = 1, message = "El número de cuotas debe ser al menos 1")
    @Column(name = "numero_cuotas", nullable = false)
    private Integer numeroCuotas;

    @Enumerated(EnumType.STRING)
    @Column(name = "intervalo_cuotas", nullable = false, length = 20)
    private IntervaloCredito intervaloCuotas;

    @NotNull(message = "La fecha de inicio es obligatoria")
    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @NotNull(message = "La fecha de fin es obligatoria")
    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    // 🔹 NUEVO CAMPO: monto inicial (opcional, puede ser 0)
    @DecimalMin(value = "0.0", message = "La inicial no puede ser negativa")
    @Column(name = "monto_inicial", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal montoInicial = BigDecimal.ZERO;

    @NotNull(message = "El monto pagado es obligatorio")
    @DecimalMin(value = "0.0", message = "El monto pagado no puede ser negativo")
    @Column(name = "monto_pagado", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal montoPagado = BigDecimal.ZERO;

    @NotNull(message = "El saldo pendiente es obligatorio")
    @DecimalMin(value = "0.0", message = "El saldo pendiente no puede ser negativo")
    @Column(name = "saldo_pendiente", nullable = false, precision = 10, scale = 2)
    private BigDecimal saldoPendiente;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    @Builder.Default
    private EstadoCredito estado = EstadoCredito.ACTIVO;

    @CreationTimestamp
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @UpdateTimestamp
    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    @OneToMany(mappedBy = "credito", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CuotaPago> cuotas = new ArrayList<>();

    @OneToMany(mappedBy = "credito", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RegistroPago> pagos = new ArrayList<>();

    // =============================
    // Métodos utilitarios
    // =============================

    public void addCuota(CuotaPago cuota) {
        cuotas.add(cuota);
        cuota.setCredito(this);
    }

    public void removeCuota(CuotaPago cuota) {
        cuotas.remove(cuota);
        cuota.setCredito(null);
    }

    public void addPago(RegistroPago pago) {
        pagos.add(pago);
        pago.setCredito(this);
    }

    public void removePago(RegistroPago pago) {
        pagos.remove(pago);
        pago.setCredito(null);
    }

    public void calcularMontoConInteres() {
        BigDecimal montoInicialSeguro = this.montoInicial != null ? this.montoInicial : BigDecimal.ZERO;
        BigDecimal interesSeguro = this.interesPorcentaje != null ? this.interesPorcentaje : BigDecimal.ZERO;
        BigDecimal saldoAFinanciar = this.montoTotal.subtract(montoInicialSeguro);

        BigDecimal montoInteres = saldoAFinanciar
                .multiply(interesSeguro)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        this.montoConInteres = saldoAFinanciar.add(montoInteres);
        this.saldoPendiente = this.montoConInteres;
    }

    public void calcularFechaFin() {
        int totalDias = this.numeroCuotas * this.intervaloCuotas.getDias();
        this.fechaFin = this.fechaInicio.plusDays(totalDias);
    }

    public BigDecimal getPorcentajePagado() {
        if (montoConInteres.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return montoPagado.multiply(BigDecimal.valueOf(100))
            .divide(montoConInteres, 2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Actualiza el estado del crédito basado en el saldo y las fechas
     */
    public void actualizarEstado() {
        if (saldoPendiente.compareTo(BigDecimal.ZERO) == 0) {
            estado = EstadoCredito.PAGADO;
        } else if (LocalDate.now().isAfter(fechaFin) && saldoPendiente.compareTo(BigDecimal.ZERO) > 0) {
            estado = EstadoCredito.VENCIDO;
        } else if (estado != EstadoCredito.CANCELADO) {
            estado = EstadoCredito.ACTIVO;
        }
    }

    public void actualizarMontosPagados() {
        BigDecimal pagadoEnCuotas = this.cuotas.stream()
            .map(CuotaPago::getMontoPagado)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.montoPagado = pagadoEnCuotas.add(this.montoInicial);
        this.saldoPendiente = this.montoConInteres.subtract(pagadoEnCuotas);  // ⬅️ Cambio aquí
    }

    @Override
    public String toString() {
        return "CreditoVenta{" +
                "id=" + id +
                ", montoTotal=" + montoTotal +
                ", montoInicial=" + montoInicial +
                ", montoConInteres=" + montoConInteres +
                ", numeroCuotas=" + numeroCuotas +
                ", montoPagado=" + montoPagado +
                ", saldoPendiente=" + saldoPendiente +
                ", estado=" + estado +
                '}';
    }
}
