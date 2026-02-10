package com.example.acceso.model.EnumVentas;

public enum MetodoPago {
    EFECTIVO("Efectivo"),
    TRANSFERENCIA("Transferencia Bancaria"),
    YAPE("Yape"),
    PLIN("Plin"),
    TARJETA("Tarjeta de Crédito/Débito");

    private final String descripcion;

    MetodoPago(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}