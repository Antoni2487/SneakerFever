package com.example.acceso.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ItemCarrito {
    private Long productoId;
    private String nombre;
    private String imagen; 
    private BigDecimal precio;
    private Integer cantidad;
    private BigDecimal subtotal;

    public void calcularSubtotal() {
        this.subtotal = this.precio.multiply(new BigDecimal(cantidad));
    }
}