package com.example.acceso.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class RucResponseDTO {
    private boolean success;
    private DatosRuc datos;

    @Data
    public static class DatosRuc {
        private String ruc;

        @JsonProperty("razon_social")
        private String razonSocial;

        private String estado;
        private String condicion;
    }
}