package com.example.acceso.model.EnumInventario;

public enum TipoReferencia {
    VENTA("Venta"),
    COMPRA("Compra"),
    AJUSTE("Ajuste"),
    NINGUNO("Sin referencia");

    private final String descripcion;

    TipoReferencia(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}