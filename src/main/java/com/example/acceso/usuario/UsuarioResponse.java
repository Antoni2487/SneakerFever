package com.example.acceso.usuario;

import com.example.acceso.perfil.PerfilResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioResponse {

    private Long id;
    private String nombre;
    private String usuario;
    private String correo;
    private Integer estado;
    private PerfilResponse perfil;

    public static UsuarioResponse from(Usuario usuario) {
        return UsuarioResponse.builder()
                .id(usuario.getId())
                .nombre(usuario.getNombre())
                .usuario(usuario.getUsuario())
                .correo(usuario.getCorreo())
                .estado(usuario.getEstado())
                .perfil(usuario.getPerfil() != null ? PerfilResponse.from(usuario.getPerfil()) : null)
                .build();
    }
}
