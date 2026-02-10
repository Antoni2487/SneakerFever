package com.example.acceso.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "personalizacion")
public class Personalizacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El tipo es obligatorio")
    @Column(nullable = false, length = 20)
    private String tipo; // "LOGO" o "SLIDE"

    @Size(max = 500, message = "La URL de la imagen no puede exceder los 500 caracteres")
    @Column(name = "imagen_url", length = 500)
    private String imagenUrl;

    @Min(value = 1, message = "El orden debe ser mínimo 1")
    @Max(value = 5, message = "El orden debe ser máximo 5")
    @Column(name = "orden")
    private Integer orden; // Solo para slides (1-5), null para logo

    // Relación con marca (solo para slides)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "marca_id")
    @JsonIgnoreProperties({"imagenes", "descripcion"}) // Evita traer data innecesaria
    private Brand marca;

    @CreationTimestamp
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @UpdateTimestamp
    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    @Override
    public String toString() {
        return "Personalizacion{" +
                "id=" + id +
                ", tipo='" + tipo + '\'' +
                ", orden=" + orden +
                ", marcaId=" + (marca != null ? marca.getId() : null) +
                ", fechaCreacion=" + fechaCreacion +
                '}';
    }
}