package com.example.acceso.dto;

import com.example.acceso.model.EnumInventario.MotivoMovimiento;
import com.example.acceso.model.EnumInventario.TipoMovimiento;
import com.example.acceso.model.EnumInventario.TipoReferencia;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovimientoInventarioResponse {

    private Long id;
    private Long productoId;
    private String productoNombre;
    private TipoMovimiento tipoMovimiento;
    private Integer cantidad;
    private Integer stockAnterior;
    private Integer stockNuevo;
    private MotivoMovimiento motivo;
    private Long referenciaId;
    private TipoReferencia referenciaTipo;
    private String observaciones;
    private Long usuarioId;
    private String usuarioNombre;
    private LocalDateTime fechaMovimiento;
}