package com.example.acceso.dto;

import com.example.acceso.model.Perfil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerfilResponse {

    private Long id;
    private String nombre;
    private String descripcion;
    private boolean estado;
    private List<OpcionResponse> opciones;

    public static PerfilResponse from(Perfil perfil) {
        return PerfilResponse.builder()
                .id(perfil.getId())
                .nombre(perfil.getNombre())
                .descripcion(perfil.getDescripcion())
                .estado(perfil.isEstado())
                .opciones(perfil.getOpciones().stream()
                        .map(OpcionResponse::from)
                        .toList())
                .build();
    }
}
