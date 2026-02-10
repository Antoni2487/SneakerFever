package com.example.acceso.model.EnumInventario;

public enum TipoMovimiento {
    ENTRADA("Entrada"),
    SALIDA("Salida"),
    DEVOLUCION("Devolución"),
    MERMA("Merma");

    private final String descripcion;

    TipoMovimiento(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}