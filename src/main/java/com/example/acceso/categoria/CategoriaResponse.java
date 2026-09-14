package com.example.acceso.categoria;

import com.example.acceso.categoria.Categoria;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoriaResponse {

    private Long id;
    private String nombre;
    private String descripcion;
    private Integer estado;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;

    public static CategoriaResponse from(Categoria categoria) {
        return CategoriaResponse.builder()
                .id(categoria.getId())
                .nombre(categoria.getNombre())
                .descripcion(categoria.getDescripcion())
                .estado(categoria.getEstado())
                .fechaCreacion(categoria.getFechaCreacion())
                .fechaActualizacion(categoria.getFechaActualizacion())
                .build();
    }
}
