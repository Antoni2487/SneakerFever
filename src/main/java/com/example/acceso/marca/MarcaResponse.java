package com.example.acceso.marca;

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
public class MarcaResponse {

    private Long id;
    private String nombre;
    private String descripcion;
    private Integer estado;
    private String imagen;
    private List<String> imagenes;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;

    public static MarcaResponse from(Marca marca) {
        return MarcaResponse.builder()
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
