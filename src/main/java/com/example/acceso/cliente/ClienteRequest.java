package com.example.acceso.cliente;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ClienteRequest {

    @NotBlank(message = "El nombre del cliente es obligatorio")
    @Size(min = 2, max = 200, message = "El nombre debe tener entre 2 y 200 caracteres")
    private String nombre;

    @NotBlank(message = "El documento es obligatorio")
    @Pattern(regexp = "^[0-9]{8}$|^[0-9]{11}$", message = "El documento debe ser un DNI (8 dígitos) o RUC (11 dígitos)")
    private String documento;

    @NotBlank(message = "El teléfono es obligatorio")
    @Pattern(regexp = "^[0-9]{9}$", message = "El teléfono debe tener 9 dígitos")
    private String telefono;

    @Email(message = "El correo debe ser válido")
    @Size(max = 100, message = "El correo no puede exceder los 100 caracteres")
    private String correo;

    public Cliente toEntity() {
        return Cliente.builder()
                .nombre(nombre)
                .documento(documento)
                .telefono(telefono)
                .correo(correo)
                .build();
    }
}
