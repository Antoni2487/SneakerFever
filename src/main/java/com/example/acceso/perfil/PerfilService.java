package com.example.acceso.perfil;

import com.example.acceso.perfil.PerfilRequest;
import com.example.acceso.perfil.Perfil;
import com.example.acceso.perfil.Opcion;

import java.util.List;
import java.util.Optional;

public interface PerfilService {
    List<Perfil> listarPerfilesActivos();

    List<Perfil> listarTodosLosPerfiles();

    Perfil guardarPerfil(Perfil perfil);

    Perfil guardarPerfil(PerfilRequest request);

    Optional<Perfil> obtenerPerfilPorId(Long id);

    Optional<Perfil> cambiarEstadoPerfil(Long id);

    List<Opcion> listarTodasLasOpciones();

    void eliminarPerfil(Long id);
}