package com.example.acceso.perfil;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Set;

@Data
public class PerfilRequest {

    private Long id;

    @NotBlank(message = "El nombre del perfil es obligatorio")
    private String nombre;

    @Size(max = 255, message = "La descripción no puede exceder los 255 caracteres")
    private String descripcion;

    private Boolean estado = true;

    // Sin inicializador a propósito: debe quedar null si el JSON no manda la
    // clave "opciones", para distinguir "no tocar permisos" de "vaciar permisos".
    private Set<Long> opciones;
}
