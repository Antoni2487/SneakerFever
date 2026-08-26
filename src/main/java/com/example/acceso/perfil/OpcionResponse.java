package com.example.acceso.perfil;

import com.example.acceso.perfil.Opcion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpcionResponse {

    private Long id;
    private String nombre;
    private String ruta;
    private String icono;

    public static OpcionResponse from(Opcion opcion) {
        return OpcionResponse.builder()
                .id(opcion.getId())
                .nombre(opcion.getNombre())
                .ruta(opcion.getRuta())
                .icono(opcion.getIcono())
                .build();
    }
}
