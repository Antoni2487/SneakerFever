package com.example.acceso.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class DniResponseDTO {
    private boolean success;
    private DatosDni datos;

    @Data
    public static class DatosDni {
        private String dni;
        private String nombres;

        @JsonProperty("ape_paterno")
        private String apePaterno;

        @JsonProperty("ape_materno")
        private String apeMaterno;

        public String getNombreCompleto() {
            return String.format("%s %s %s", nombres, apePaterno, apeMaterno).trim();
        }
    }
}