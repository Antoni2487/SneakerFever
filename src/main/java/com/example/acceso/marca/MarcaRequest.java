package com.example.acceso.marca;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MarcaRequest {

    @NotBlank(message = "El nombre de la marca es obligatorio")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    private String nombre;

    @Size(max = 500, message = "La descripción no puede exceder los 500 caracteres")
    private String descripcion;

    @Size(max = 500, message = "La URL de la imagen no puede exceder los 500 caracteres")
    private String imagen;

    public Marca toEntity() {
        return Marca.builder()
                .nombre(nombre)
                .descripcion(descripcion)
                .imagen(imagen)
                .build();
    }
}
