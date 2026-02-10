package com.example.acceso.model.EnumVentas;

import lombok.Getter;

@Getter
public enum EstadoCuota {
    PENDIENTE("Pendiente", "Cuota pendiente de pago"),
    PAGADA("Pagada", "Cuota pagada completamente"),
    PARCIAL("Parcial", "Cuota con pago parcial"),
    VENCIDA("Vencida", "Cuota con fecha de vencimiento superada");

    private final String nombre;
    private final String descripcion;

    EstadoCuota(String nombre, String descripcion) {
        this.nombre = nombre;
        this.descripcion = descripcion;
    }
}