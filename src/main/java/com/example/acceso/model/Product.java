package com.example.acceso.model;

import com.example.acceso.model.EntidadesVenta.DetalleVenta;
import com.fasterxml.jackson.annotation.JsonIgnore; 
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "productos")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre del producto es obligatorio")
    @Size(min = 2, max = 200, message = "El nombre debe tener entre 2 y 200 caracteres")
    @Column(name = "nombre", nullable = false, length = 200)
    private String nombre;

    @Size(max = 1000, message = "La descripción no puede exceder los 1000 caracteres")
    @Column(name = "descripcion", length = 1000)
    private String descripcion;

    @Size(max = 500, message = "La URL de la imagen no puede exceder los 500 caracteres")
    @Column(name = "imagen", length = 500)
    private String imagen;
    
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "producto_imagenes",
        joinColumns = @JoinColumn(name = "producto_id")
    )
    @Column(name = "url", length = 500)
    @OrderColumn(name = "orden")
    private List<String> imagenes = new ArrayList<>();

    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "El precio debe ser mayor a 0")
    @Column(name = "precio", nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @DecimalMin(value = "0.0", message = "El descuento no puede ser negativo")
    @DecimalMax(value = "100.0", message = "El descuento no puede ser mayor a 100%")
    @Column(name = "descuento", precision = 5, scale = 2)
    private BigDecimal descuento;

    @NotNull(message = "El campo destacado es obligatorio")
    @Column(name = "destacado", nullable = false)
    private Boolean destacado = false;

    @NotNull(message = "El stock es obligatorio")
    @Min(value = 0, message = "El stock no puede ser negativo")
    @Column(name = "stock", nullable = false)
    private Integer stock = 0;

    @NotNull(message = "El stock mínimo es obligatorio")
    @Min(value = 0, message = "El stock mínimo no puede ser negativo")
    @Column(name = "stock_minimo", nullable = false)
    private Integer stockMinimo = 5;

    @NotNull(message = "El género es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "genero", nullable = false, length = 20)
    private Genero genero;

    @NotNull(message = "La categoría es obligatoria")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_categoria", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Category category;

    @NotNull(message = "La marca es obligatoria")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_marca", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Brand brand;

    @Column(nullable = false)
    private Integer estado = 1;

    @CreationTimestamp
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @UpdateTimestamp
    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;


    @JsonIgnore
    @OneToMany(mappedBy = "producto", fetch = FetchType.LAZY)
    @Builder.Default
    private List<DetalleVenta> detalles = new ArrayList<>();

    public boolean tieneStock(Integer cantidad) {
        return stock >= cantidad;
    }

    public void descontarStock(Integer cantidad) {
        if (!tieneStock(cantidad)) {
            throw new IllegalStateException(
                "Stock insuficiente. Disponible: " + stock + ", Requerido: " + cantidad
            );
        }
        this.stock -= cantidad;
    }

    public void incrementarStock(Integer cantidad) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad a incrementar debe ser mayor a 0");
        }
        this.stock += cantidad;
    }

    public boolean stockBajo() {
        return stock <= stockMinimo;
    }

    @Override
    public String toString() {
        return "Product{" +
                "id=" + id +
                ", nombre='" + nombre + '\'' +
                ", precio=" + precio +
                ", descuento=" + descuento +
                ", destacado=" + destacado +
                ", stock=" + stock +
                ", stockMinimo=" + stockMinimo +
                ", genero=" + genero +
                ", estado=" + estado +
                ", fechaCreacion=" + fechaCreacion +
                '}';
    }

    public void setImagenes(List<String> imagenes) {
        this.imagenes = imagenes;
        if (imagenes != null && !imagenes.isEmpty()) {
            this.imagen = imagenes.get(0);
        }
    }
}