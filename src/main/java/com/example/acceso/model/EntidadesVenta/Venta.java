package com.example.acceso.model.EntidadesVenta;

import com.example.acceso.model.Cliente;
import com.example.acceso.model.EnumVentas.EstadoVenta;
import com.example.acceso.model.EnumVentas.FormaPago;
import com.example.acceso.model.EnumVentas.TipoComprobante;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "ventas")
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relación con Cliente
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    // Información del Comprobante
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_comprobante", nullable = false, length = 20)
    private TipoComprobante tipoComprobante;

    @NotBlank(message = "La serie es obligatoria")
    @Size(max = 4, message = "La serie no puede exceder 4 caracteres")
    @Column(name = "serie", nullable = false, length = 4)
    private String serie;

    @NotBlank(message = "El número de comprobante es obligatorio")
    @Size(max = 8, message = "El número no puede exceder 8 caracteres")
    @Column(name = "numero", nullable = false, length = 8)
    private String numero;

    // Información de Forma de Pago
    @Enumerated(EnumType.STRING)
    @Column(name = "forma_pago", nullable = false, length = 20)
    private FormaPago formaPago;

    // Montos
    @NotNull(message = "El subtotal es obligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "El subtotal debe ser mayor a 0")
    @Column(name = "subtotal", nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @DecimalMin(value = "0.0", message = "El descuento general no puede ser negativo")
    @DecimalMax(value = "100.0", message = "El descuento general no puede exceder 100%")
    @Column(name = "descuento_general", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal descuentoGeneral = BigDecimal.ZERO;

    @NotNull(message = "El IGV es obligatorio")
    @DecimalMin(value = "0.0", message = "El IGV no puede ser negativo")
    @Column(name = "igv", nullable = false, precision = 10, scale = 2)
    private BigDecimal igv;

    @NotNull(message = "El total es obligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "El total debe ser mayor a 0")
    @Column(name = "total", nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    // Estado y Observaciones
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    @Builder.Default
    private EstadoVenta estado = EstadoVenta.PENDIENTE;

    @Size(max = 500, message = "Las observaciones no pueden exceder 500 caracteres")
    @Column(name = "observaciones", length = 500)
    private String observaciones;

    // Auditoría
    @NotBlank(message = "El usuario de creación es obligatorio")
    @Size(max = 100, message = "El usuario de creación no puede exceder 100 caracteres")
    @Column(name = "usuario_creacion", nullable = false, length = 100)
    private String usuarioCreacion;

    @CreationTimestamp
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @UpdateTimestamp
    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    // Relaciones
    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DetalleVenta> detalles = new ArrayList<>();

    @OneToOne(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true)
    private CreditoVenta credito;

    // Métodos de utilidad
    public void addDetalle(DetalleVenta detalle) {
        detalles.add(detalle);
        detalle.setVenta(this);
    }

    public void removeDetalle(DetalleVenta detalle) {
        detalles.remove(detalle);
        detalle.setVenta(null);
    }

    public String getComprobanteCompleto() {
        return serie + "-" + numero;
    }

    public boolean esCredito() {
        return formaPago == FormaPago.CREDITO;
    }

    public void calcularTotal() {
        if (subtotal == null) {
            throw new IllegalStateException("El subtotal no puede ser nulo");
        }

        // Aplicar descuento general
        BigDecimal montoDescuento = subtotal.multiply(descuentoGeneral)
            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal subtotalConDescuento = subtotal.subtract(montoDescuento);

        // SIN IGV
        this.igv = BigDecimal.ZERO;
        this.total = subtotalConDescuento.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public String toString() {
        return "Venta{" +
                "id=" + id +
                ", tipoComprobante=" + tipoComprobante +
                ", serie='" + serie + '\'' +
                ", numero='" + numero + '\'' +
                ", formaPago=" + formaPago +
                ", total=" + total +
                ", estado=" + estado +
                ", fechaCreacion=" + fechaCreacion +
                '}';
    }
}
