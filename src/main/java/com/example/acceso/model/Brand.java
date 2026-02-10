package com.example.acceso.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "marcas")
public class Brand {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre de la marca es obligatorio")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Size(max = 500, message = "La descripción no puede exceder los 500 caracteres")
    @Column(name = "descripcion", length = 500)
    private String descripcion;

    @Column(nullable = false)
    private Integer estado = 1;

    @Size(max = 500, message = "La URL de la imagen no puede exceder los 500 caracteres")
    @Column(name = "imagen", length = 500)
    private String imagen;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "brand_imagenes",  // ← Cambiar a minúsculas (convención)
        joinColumns = @JoinColumn(name = "marca_id")  // ← Cambiar de producto_id a marca_id
    )
    @Column(name = "url", length = 500)
    @OrderColumn(name = "orden")
    private List<String> imagenes = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @UpdateTimestamp
    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;


    @Override
    public String toString() {
        return "Brand{" +
                "id=" + id +
                ", nombre='" + nombre + '\'' +
                ", descripcion='" + descripcion + '\'' +
                ", estado=" + estado +
                ", fechaCreacion=" + fechaCreacion +
                ", fechaActualizacion=" + fechaActualizacion +
                '}';
    }


       public void setImagenes(List<String> imagenes) {
           this.imagenes = imagenes;
           // Sincronizar imagen principal
           if (imagenes != null && !imagenes.isEmpty()) {
               this.imagen = imagenes.get(0);
           }
       }
}
