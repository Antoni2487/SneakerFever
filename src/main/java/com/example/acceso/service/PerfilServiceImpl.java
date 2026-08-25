package com.example.acceso.service;

import com.example.acceso.dto.PerfilRequest;
import com.example.acceso.model.Perfil;
import com.example.acceso.model.Opcion;
import com.example.acceso.repository.PerfilRepository;
import com.example.acceso.repository.OpcionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class PerfilServiceImpl implements PerfilService {

    private final PerfilRepository perfilRepository;
    private final OpcionRepository opcionRepository;

    public PerfilServiceImpl(PerfilRepository perfilRepository, OpcionRepository opcionRepository) {
        this.perfilRepository = perfilRepository;
        this.opcionRepository = opcionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Perfil> listarPerfilesActivos() {
        return perfilRepository.findByEstadoTrue();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Perfil> listarTodosLosPerfiles() {
        return perfilRepository.findAll();
    }

    @Override
    @Transactional
    public Perfil guardarPerfil(Perfil perfil) {
        return perfilRepository.save(perfil);
    }

    @Override
    @Transactional
    public Perfil guardarPerfil(PerfilRequest request) {
        Perfil perfil = request.getId() != null
                ? perfilRepository.findById(request.getId()).orElse(new Perfil())
                : new Perfil();

        perfil.setNombre(request.getNombre());
        perfil.setDescripcion(request.getDescripcion());
        perfil.setEstado(request.getEstado() != null ? request.getEstado() : true);

        if (request.getOpciones() != null) {
            Set<Opcion> opciones = new HashSet<>(opcionRepository.findAllById(request.getOpciones()));
            perfil.setOpciones(opciones);
        }

        return perfilRepository.save(perfil);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Perfil> obtenerPerfilPorId(Long id) {
        return perfilRepository.findById(id);
    }

    @Override
    @Transactional
    public Optional<Perfil> cambiarEstadoPerfil(Long id) {
        return perfilRepository.findById(id).map(perfil -> {
            perfil.setEstado(!perfil.isEstado());
            return perfilRepository.save(perfil);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<Opcion> listarTodasLasOpciones() {
        return opcionRepository.findAll();
    }

    @Override
    @Transactional
    public void eliminarPerfil(Long id) {
        perfilRepository.deleteById(id);
    }
}