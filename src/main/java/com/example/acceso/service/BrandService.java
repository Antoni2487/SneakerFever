package com.example.acceso.service;

import com.example.acceso.model.Brand;
import com.example.acceso.repository.BrandRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class BrandService {

    private final BrandRepository brandRepository;

    public BrandService(BrandRepository brandRepository) {
        this.brandRepository = brandRepository;
    }

    // ===================== Listados =====================
    @Transactional(readOnly = true)
    public List<Brand> listarMarcas() {
        return brandRepository.findAllByEstado(1);
    }

    @Transactional(readOnly = true)
    public List<Brand> listarTodasMarcas() {
        return brandRepository.findAllWithImages();
    }

    // ===================== Guardar / Actualizar =====================
    @Transactional
    public Brand guardarMarca(Brand marca) {
        validarMarca(marca);

        try {
            // Normalizar texto
            marca.setNombre(marca.getNombre().trim());

            if (marca.getDescripcion() != null) {
                marca.setDescripcion(marca.getDescripcion().trim());
                if (marca.getDescripcion().isEmpty()) {
                    marca.setDescripcion(null);
                }
            }

            if (marca.getId() != null) {
                // Actualización
                Brand existente = obtenerMarcaPorId(marca.getId())
                        .orElseThrow(() -> new MarcaException("Marca no encontrada para actualizar"));

                if (marca.getEstado() == null) {
                    marca.setEstado(existente.getEstado());
                }

                // Mantener la imagen existente si no se proporciona una nueva
                if (marca.getImagen() == null) {
                    marca.setImagen(existente.getImagen());
                }
            } else {
                // Nueva marca
                marca.setEstado(1);
            }

            return brandRepository.save(marca);

        } catch (DataIntegrityViolationException e) {
            String msg = e.getMessage().toLowerCase();
            if (msg.contains("nombre") || msg.contains("unique")) {
                throw new MarcaException("Ya existe una marca con ese nombre");
            }
            throw new MarcaException("Error de integridad de datos");
        } catch (Exception e) {
            throw new MarcaException("Error al guardar la marca: " + e.getMessage(), e);
        }
    }

    // ===================== Contadores =====================
    @Transactional(readOnly = true)
    public long contarMarcas() {
        return brandRepository.countByEstado(1);
    }

    @Transactional(readOnly = true)
    public long contarTodasMarcas() {
        return brandRepository.count();
    }

    // ===================== Obtención =====================
    @Transactional(readOnly = true)
    public Optional<Brand> obtenerMarcaPorId(Long id) {
        if (id == null || id <= 0) return Optional.empty();
        return brandRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Brand> findByNombre(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) return Optional.empty();
        return brandRepository.findByNombre(nombre.trim());
    }

    // ===================== Eliminación / Estado =====================
    @Transactional
    public void eliminarMarca(Long id) {
        Brand marca = obtenerMarcaPorId(id)
                .orElseThrow(() -> new MarcaException("Marca no encontrada"));
        marca.setEstado(2);
        brandRepository.save(marca);
    }

    @Transactional
    public Optional<Brand> cambiarEstadoMarca(Long id) {
        if (id == null || id <= 0) return Optional.empty();
        return obtenerMarcaPorId(id).map(m -> {
            m.setEstado(m.getEstado() == 1 ? 0 : 1);
            return brandRepository.save(m);
        });
    }

    // ===================== Existencias =====================
    @Transactional(readOnly = true)
    public boolean existeMarca(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) return false;
        return brandRepository.existsByNombreAndEstadoNot(nombre.trim(), 2); // 🔥 CAMBIAR ESTA LÍNEA
    }

    @Transactional(readOnly = true)
    public boolean existeMarcaParaActualizar(String nombre, Long id) {
        if (nombre == null || nombre.trim().isEmpty()) return false;
        return brandRepository.existsByNombreAndIdNotAndEstadoNot(nombre.trim(), id, 2); // 🔥 CAMBIAR ESTA LÍNEA
    }

    // ===================== Búsquedas =====================
    @Transactional(readOnly = true)
    public List<Brand> buscarMarcas(String texto) {
        if (texto == null || texto.trim().isEmpty()) return listarMarcas();
        return brandRepository.findByNombreContainingIgnoreCase(texto.trim());
    }

    @Transactional(readOnly = true)
    public List<Brand> buscarMarcasActivas(String texto) {
        if (texto == null || texto.trim().isEmpty()) return listarMarcas();
        return brandRepository.findByNombreContainingIgnoreCaseAndEstado(texto.trim(), 1);
    }

    // ===================== Gestión de Imágenes =====================

    /**
     * Actualiza la URL de la imagen de una marca
     * Se usa después de subir la imagen con FileUploadController
     */
    @Transactional
    public Brand actualizarImagen(Long id, String imagenUrl) {
        if (id == null || id <= 0) {
            throw new MarcaException("ID de marca inválido");
        }

        if (imagenUrl == null || imagenUrl.trim().isEmpty()) {
            throw new MarcaException("La URL de la imagen no puede estar vacía");
        }

        Brand marca = obtenerMarcaPorId(id)
                .orElseThrow(() -> new MarcaException("Marca no encontrada con ID: " + id));

        marca.setImagen(imagenUrl.trim());
        return brandRepository.save(marca);
    }

    /**
     * Elimina la referencia de imagen de una marca
     * Nota: El archivo físico debe eliminarse usando FileUploadController
     */
    @Transactional
    public Brand eliminarImagen(Long id) {
        if (id == null || id <= 0) {
            throw new MarcaException("ID de marca inválido");
        }

        Brand marca = obtenerMarcaPorId(id)
                .orElseThrow(() -> new MarcaException("Marca no encontrada con ID: " + id));

        marca.setImagen(null);
        return brandRepository.save(marca);
    }

    /**
     * Obtiene la URL de la imagen de una marca
     */
    @Transactional(readOnly = true)
    public Optional<String> obtenerImagenUrl(Long id) {
        if (id == null || id <= 0) return Optional.empty();

        return obtenerMarcaPorId(id)
                .map(Brand::getImagen)
                .filter(url -> url != null && !url.trim().isEmpty());
    }

    /**
     * Lista marcas que tienen imagen
     */
    @Transactional(readOnly = true)
    public List<Brand> listarMarcasConImagen() {
        return brandRepository.findByImagenIsNotNullAndEstado(1);
    }

    /**
     * Lista marcas sin imagen
     */
    @Transactional(readOnly = true)
    public List<Brand> listarMarcasSinImagen() {
        return brandRepository.findByImagenIsNullAndEstado(1);
    }

    // ===================== Validaciones =====================
    private void validarMarca(Brand marca) {
        if (marca == null) throw new MarcaException("Marca no puede ser nula");
        if (marca.getNombre() == null || marca.getNombre().trim().isEmpty()) {
            throw new MarcaException("El nombre de la marca es obligatorio");
        }
    }

    // ===================== Excepción personalizada =====================
    public static class MarcaException extends RuntimeException {
        public MarcaException(String message) {
            super(message);
        }
        public MarcaException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}