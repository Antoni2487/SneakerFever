package com.example.acceso.model.EntidadesVenta;

import com.example.acceso.model.EnumVentas.TipoComprobante;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(
    name = "comprobantes_secuencia",
    uniqueConstraints = @UniqueConstraint(columnNames = {"tipo_comprobante", "serie"})
)
public class ComprobanteSecuencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_comprobante", nullable = false, length = 20)
    private TipoComprobante tipoComprobante;

    @Column(name = "serie", nullable = false, length = 4)
    private String serie;

    @Column(name = "numero_actual", nullable = false)
    private Long numeroActual = 0L;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @UpdateTimestamp
    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    // ===================== MÉTODOS PRINCIPALES =====================

    /** Incrementa el número y devuelve el siguiente número formateado (ej. 00000025) */
    public String generarSiguienteNumero() {
        numeroActual++;
        return String.format("%08d", numeroActual);
    }

    /** Muestra el siguiente número sin modificar el contador */
    public String visualizarSiguienteNumero() {
        return String.format("%08d", numeroActual + 1);
    }

    /** Devuelve la serie y número combinados (ej. F001-00000025) */
    public String generarComprobanteCompleto() {
        return serie + "-" + generarSiguienteNumero();
    }
}
