package com.example.acceso.model.EnumVentas;
import lombok.Getter;

@Getter

public enum FormaPago {
    CONTADO("Contado"),
    CREDITO("Crédito");

    private final String descripcion;

    FormaPago(String descripcion) {
        this.descripcion = descripcion;
    }
}
