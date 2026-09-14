package com.example.acceso.categoria;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;


@Service
public class CategoriaService {

    private final CategoriaRepository categoryRepository;

    public CategoriaService(CategoriaRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    // ===================== Listados =====================
    @Transactional(readOnly = true)
    public List<Categoria> listarCategorias() {
        return categoryRepository.findAllByEstado(1);
    }

    @Transactional(readOnly = true)
    public List<Categoria> listarTodasCategorias() {
        return categoryRepository.findAllByEstadoNot(2);
    }

    // ===================== Guardar / Actualizar =====================
    @Transactional
    public Categoria guardarCategoria(Categoria categoria) {
        validarCategoria(categoria);

        try {
            // ===================== Normalizar texto =====================
            categoria.setNombre(categoria.getNombre().trim());
            if (categoria.getDescripcion() != null) {
                categoria.setDescripcion(categoria.getDescripcion().trim());
                if (categoria.getDescripcion().isEmpty()) {
                    categoria.setDescripcion(null);
                }
            }

            // ===================== Crear o Actualizar =====================
            if (categoria.getId() != null) {
                // Actualización
                Optional<Categoria> existente = obtenerCategoriaPorId(categoria.getId());
                if (existente.isPresent()) {
                    // Si no se envía un estado, mantener el existente
                    if (categoria.getEstado() == null) {
                        categoria.setEstado(existente.get().getEstado());
                    }
                } else {
                    throw new CategoriaException("Categoría no encontrada para actualizar");
                }
            } else {
                // Nueva categoría → estado = 1 (activa)
                categoria.setEstado(1);
            }

            // ===================== Guardar =====================
            return categoryRepository.save(categoria);

        } catch (DataIntegrityViolationException e) {
            String msg = e.getMessage().toLowerCase();
            if (msg.contains("nombre") || msg.contains("unique")) {
                throw new CategoriaException("Ya existe una categoría con ese nombre");
            }
            throw new CategoriaException("Error de integridad de datos");
        } catch (Exception e) {
            throw new CategoriaException("Error al guardar la categoría: " + e.getMessage(), e);
        }
    }


    // ===================== Contadores =====================
    @Transactional(readOnly = true)
    public long contarCategorias() {
        // 🔹 Contar solo las activas (estado = 1)
        return categoryRepository.countByEstado(1);
    }

    @Transactional(readOnly = true)
    public long contarTodasCategorias() {
        // 🔹 Contar todas menos las eliminadas (estado != 2)
        return categoryRepository.countByEstadoNot(2);
    }


    // ===================== Obtención =====================
    @Transactional(readOnly = true)
    public Optional<Categoria> obtenerCategoriaPorId(Long id) {
        if (id == null || id <= 0) return Optional.empty();
        return categoryRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Categoria> findByNombre(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) return Optional.empty();
        return categoryRepository.findByNombre(nombre.trim());
    }

    // ===================== Eliminación / Estado =====================
    @Transactional
    public void eliminarCategoria(Long id) {
        Categoria categoria = obtenerCategoriaPorId(id)
                .orElseThrow(() -> new CategoriaException("Categoría no encontrada"));

        categoria.setEstado(2); // 🔹 2 = Eliminado
        categoryRepository.save(categoria);
    }

    @Transactional
    public Optional<Categoria> cambiarEstadoCategoria(Long id) {
        if (id == null || id <= 0) return Optional.empty();

        return obtenerCategoriaPorId(id).map(c -> {
            // 🔹 Cambiar entre 1 (activo) y 0 (inactivo)
            if (c.getEstado() == 1) {
                c.setEstado(0);
            } else if (c.getEstado() == 0) {
                c.setEstado(1);
            }
            return categoryRepository.save(c);
        });
    }

    // ===================== Existencias =====================
    @Transactional(readOnly = true)
    public boolean existeCategoria(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) return false;
        return categoryRepository.existsByNombre(nombre.trim());
    }

    @Transactional(readOnly = true)
    public boolean existeCategoriaParaActualizar(String nombre, Long id) {
        if (nombre == null || nombre.trim().isEmpty()) return false;
        return categoryRepository.existsByNombreAndIdNot(nombre.trim(), id);
    }

    // ===================== Búsquedas =====================
    @Transactional(readOnly = true)
    public List<Categoria> buscarCategorias(String texto) {
        if (texto == null || texto.trim().isEmpty()) return listarCategorias();
        return categoryRepository.findByNombreContainingIgnoreCase(texto.trim());
    }

    @Transactional(readOnly = true)
    public List<Categoria> buscarCategoriasActivas(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return categoryRepository.findAllByEstado(1);
        }
        return categoryRepository.findByNombreContainingIgnoreCaseAndEstado(texto.trim(), 1);
    }


    // ===================== Validaciones =====================
    private void validarCategoria(Categoria categoria) {
        if (categoria == null) throw new CategoriaException("Categoría no puede ser nula");
        if (categoria.getNombre() == null || categoria.getNombre().trim().isEmpty()) {
            throw new CategoriaException("El nombre de la categoría es obligatorio");
        }
    }

    // ===================== Excepción personalizada =====================
    public static class CategoriaException extends RuntimeException {
        public CategoriaException(String message) {
            super(message);
        }
        public CategoriaException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

