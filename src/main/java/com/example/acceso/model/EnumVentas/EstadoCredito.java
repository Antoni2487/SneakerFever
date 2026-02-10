package com.example.acceso.model.EnumVentas;

import lombok.Getter;

@Getter
public enum EstadoCredito {
    ACTIVO("Activo", "Crédito vigente con pagos pendientes"),
    PAGADO("Pagado", "Crédito totalmente cancelado"),
    VENCIDO("Vencido", "Crédito con pagos atrasados"),
    CANCELADO("Cancelado", "Crédito cancelado por anulación de venta");

    private final String nombre;
    private final String descripcion;

    EstadoCredito(String nombre, String descripcion) {
        this.nombre = nombre;
        this.descripcion = descripcion;
    }
}
