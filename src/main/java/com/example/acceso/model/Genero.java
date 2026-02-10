package com.example.acceso.model;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum Genero {
    HOMBRE("Hombre"),
    MUJER("Mujer");

    @JsonCreator
    public static Genero fromString(String value) {
        // Normalizamos a mayúsculas
        final String normalizedValue = value.toUpperCase();

        // Ahora aceptamos HOMBRE/MUJER (del formulario) Y MASCULINO/FEMENINO (si hay otras fuentes)
        switch (normalizedValue) {
            case "HOMBRE":
            case "MASCULINO":
                return HOMBRE;
            case "MUJER":
            case "FEMENINO":
                return MUJER;
            default:
                throw new IllegalArgumentException("Valor de género no válido: " + value);
        }
    }

    private final String descripcion;

    Genero(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }

    @Override
    public String toString() {
        return descripcion;
    }
}