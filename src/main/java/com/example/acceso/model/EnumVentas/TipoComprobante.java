package com.example.acceso.model.EnumVentas;

import lombok.Getter;

@Getter
public enum TipoComprobante {
    BOLETA("Boleta", "B"),
    FACTURA("Factura", "F"),
    NOTA_VENTA("Nota de Venta", "NV");

    private final String descripcion;
    private final String codigo;

    TipoComprobante(String descripcion, String codigo) {
        this.descripcion = descripcion;
        this.codigo = codigo;
    }
}
