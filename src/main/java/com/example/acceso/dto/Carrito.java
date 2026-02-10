package com.example.acceso.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class Carrito {
    private List<ItemCarrito> items = new ArrayList<>();
    private BigDecimal total = BigDecimal.ZERO;

    public void agregarItem(ItemCarrito nuevoItem) {
        // Si ya existe, sumamos cantidad
        for (ItemCarrito item : items) {
            if (item.getProductoId().equals(nuevoItem.getProductoId())) {
                item.setCantidad(item.getCantidad() + nuevoItem.getCantidad());
                item.calcularSubtotal();
                calcularTotal();
                return;
            }
        }
     
        this.items.add(nuevoItem);
        calcularTotal();
    }

    public void eliminarItem(Long productoId) {
        items.removeIf(i -> i.getProductoId().equals(productoId));
        calcularTotal();
    }
    
    public void actualizarCantidad(Long productoId, int cantidad) {
        for (ItemCarrito item : items) {
            if (item.getProductoId().equals(productoId)) {
                item.setCantidad(cantidad);
                item.calcularSubtotal();
                break;
            }
        }
        calcularTotal();
    }

    private void calcularTotal() {
        total = BigDecimal.ZERO;
        for (ItemCarrito item : items) {
            total = total.add(item.getSubtotal());
        }
    }
}