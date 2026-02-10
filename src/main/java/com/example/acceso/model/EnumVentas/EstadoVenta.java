package com.example.acceso.model.EnumVentas;

import lombok.Getter;

@Getter
public enum EstadoVenta {
    PENDIENTE("Pendiente"),    // Venta a CRÉDITO - aún no pagada completamente
    PAGADA("Pagada"),          // Venta CONTADO o crédito totalmente pagado
    ANULADA("Anulada");        // Venta cancelada

    private final String descripcion;

    EstadoVenta(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}

