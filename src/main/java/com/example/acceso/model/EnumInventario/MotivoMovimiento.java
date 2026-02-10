package com.example.acceso.model.EnumInventario;

public enum MotivoMovimiento {
    COMPRA("Compra de mercadería"),
    VENTA("Venta al cliente"),
    AJUSTE_FISICO("Ajuste por inventario físico"),
    MERMA("Producto vencido/dañado"),
    DEVOLUCION_CLIENTE("Devolución de cliente"),
    AJUSTE_POSITIVO("Corrección positiva"),
    AJUSTE_NEGATIVO("Corrección negativa");

    private final String descripcion;

    MotivoMovimiento(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}