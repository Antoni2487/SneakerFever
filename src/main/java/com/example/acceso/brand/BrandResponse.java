package com.example.acceso.brand;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrandResponse {

    private Long id;
    private String nombre;
    private String descripcion;
    private Integer estado;
    private String imagen;
    private List<String> imagenes;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;

    public static BrandResponse from(Brand marca) {
        return BrandResponse.builder()
                .id(marca.getId())
                .nombre(marca.getNombre())
                .descripcion(marca.getDescripcion())
                .estado(marca.getEstado())
                .imagen(marca.getImagen())
                .imagenes(marca.getImagenes())
                .fechaCreacion(marca.getFechaCreacion())
                .fechaActualizacion(marca.getFechaActualizacion())
                .build();
    }
}
