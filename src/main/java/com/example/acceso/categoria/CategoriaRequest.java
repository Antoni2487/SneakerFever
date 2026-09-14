package com.example.acceso.categoria;

import com.example.acceso.categoria.Categoria;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CategoriaRequest {

    @NotBlank(message = "El nombre de la categoría es obligatorio")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    private String nombre;

    @Size(max = 500, message = "La descripción no puede exceder los 500 caracteres")
    private String descripcion;

    public Categoria toEntity() {
        return Categoria.builder()
                .nombre(nombre)
                .descripcion(descripcion)
                .build();
    }
}
