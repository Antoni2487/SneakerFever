package com.example.acceso.service;

import com.example.acceso.model.Perfil;
import com.example.acceso.model.Opcion;

import java.util.List;
import java.util.Optional;

public interface PerfilService {
    List<Perfil> listarPerfilesActivos();

    List<Perfil> listarTodosLosPerfiles();

    Perfil guardarPerfil(Perfil perfil);

    Optional<Perfil> obtenerPerfilPorId(Long id);

    Optional<Perfil> cambiarEstadoPerfil(Long id);

    List<Opcion> listarTodasLasOpciones();

    void eliminarPerfil(Long id);
}