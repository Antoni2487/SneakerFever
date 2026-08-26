package com.example.acceso.venta;

import lombok.Getter;

@Getter
public enum IntervaloCredito {
    SEMANAL("Semanal", 7),
    QUINCENAL("Quincenal", 15),
    MENSUAL("Mensual", 30);

    private final String descripcion;
    private final int dias;

    IntervaloCredito(String descripcion, int dias) {
        this.descripcion = descripcion;
        this.dias = dias;
    }
}
