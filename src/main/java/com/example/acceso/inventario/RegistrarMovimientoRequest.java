package com.example.acceso.inventario;

import com.example.acceso.inventario.MotivoMovimiento;
import com.example.acceso.inventario.TipoMovimiento;
import com.example.acceso.inventario.TipoReferencia;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistrarMovimientoRequest {

    @NotNull(message = "El ID del producto es obligatorio")
    private Long productoId;

    @NotNull(message = "El tipo de movimiento es obligatorio")
    private TipoMovimiento tipoMovimiento;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    private Integer cantidad;

    @NotNull(message = "El motivo es obligatorio")
    private MotivoMovimiento motivo;

    private Long referenciaId;

    @Builder.Default
    private TipoReferencia referenciaTipo = TipoReferencia.NINGUNO;

    private String observaciones;
}