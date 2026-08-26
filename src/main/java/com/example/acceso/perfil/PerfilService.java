package com.example.acceso.perfil;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class PerfilService {

    private final PerfilRepository perfilRepository;
    private final OpcionRepository opcionRepository;

    public PerfilService(PerfilRepository perfilRepository, OpcionRepository opcionRepository) {
        this.perfilRepository = perfilRepository;
        this.opcionRepository = opcionRepository;
    }

    @Transactional(readOnly = true)
    public List<Perfil> listarPerfilesActivos() {
        return perfilRepository.findByEstadoTrue();
    }

    @Transactional(readOnly = true)
    public List<Perfil> listarTodosLosPerfiles() {
        return perfilRepository.findAll();
    }

    @Transactional
    public Perfil guardarPerfil(Perfil perfil) {
        return perfilRepository.save(perfil);
    }

    @Transactional
    public Perfil guardarPerfil(PerfilRequest request) {
        Perfil perfil = request.getId() != null
                ? perfilRepository.findById(request.getId()).orElse(new Perfil())
                : new Perfil();

        perfil.setNombre(request.getNombre());
        perfil.setDescripcion(request.getDescripcion());
        if (request.getEstado() != null) {
            perfil.setEstado(request.getEstado());
        }

        if (request.getOpciones() != null) {
            Set<Opcion> opciones = new HashSet<>(opcionRepository.findAllById(request.getOpciones()));
            perfil.setOpciones(opciones);
        }

        return perfilRepository.save(perfil);
    }

    @Transactional(readOnly = true)
    public Optional<Perfil> obtenerPerfilPorId(Long id) {
        return perfilRepository.findById(id);
    }

    @Transactional
    public Optional<Perfil> cambiarEstadoPerfil(Long id) {
        return perfilRepository.findById(id).map(perfil -> {
            perfil.setEstado(!perfil.isEstado());
            return perfilRepository.save(perfil);
        });
    }

    @Transactional(readOnly = true)
    public List<Opcion> listarTodasLasOpciones() {
        return opcionRepository.findAll();
    }

    @Transactional
    public void eliminarPerfil(Long id) {
        perfilRepository.deleteById(id);
    }
}
