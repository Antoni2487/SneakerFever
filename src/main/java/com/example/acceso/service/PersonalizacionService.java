package com.example.acceso.service;

import com.example.acceso.model.Brand;
import com.example.acceso.model.Personalizacion;
import com.example.acceso.repository.PersonalizacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class PersonalizacionService {

    private final PersonalizacionRepository personalizacionRepository;
    private final BrandService brandService;

    private static final String TIPO_LOGO = "LOGO";
    private static final String TIPO_SLIDE = "SLIDE";
    private static final int MAX_SLIDES = 5;

    public PersonalizacionService(PersonalizacionRepository personalizacionRepository,
                                   BrandService brandService) {
        this.personalizacionRepository = personalizacionRepository;
        this.brandService = brandService;
        inicializarPersonalizacion();
    }

    // ===================== Inicialización =====================

    /**
     * Inicializa la personalización con 1 logo + 5 slots de slides vacíos
     */
    @Transactional
    public void inicializarPersonalizacion() {
        // Crear logo si no existe
        if (personalizacionRepository.findLogo().isEmpty()) {
            Personalizacion logo = Personalizacion.builder()
                    .tipo(TIPO_LOGO)
                    .build();
            personalizacionRepository.save(logo);
        }

        // Crear 5 slots de slides si no existen
        long slidesExistentes = personalizacionRepository.countByTipo(TIPO_SLIDE);
        if (slidesExistentes < MAX_SLIDES) {
            for (int i = 1; i <= MAX_SLIDES; i++) {
                if (personalizacionRepository.findByTipoAndOrden(TIPO_SLIDE, i).isEmpty()) {
                    Personalizacion slide = Personalizacion.builder()
                            .tipo(TIPO_SLIDE)
                            .orden(i)
                            .build();
                    personalizacionRepository.save(slide);
                }
            }
        }
    }

    // ===================== Logo =====================

    @Transactional(readOnly = true)
    public Optional<Personalizacion> obtenerLogo() {
        return personalizacionRepository.findLogo();
    }

    @Transactional
    public Personalizacion actualizarLogo(String imagenUrl) {
        if (imagenUrl == null || imagenUrl.trim().isEmpty()) {
            throw new PersonalizacionException("La URL de la imagen no puede estar vacía");
        }

        Personalizacion logo = personalizacionRepository.findLogo()
                .orElseThrow(() -> new PersonalizacionException("Logo no encontrado"));

        logo.setImagenUrl(imagenUrl.trim());
        return personalizacionRepository.save(logo);
    }

    @Transactional
    public Personalizacion eliminarLogo() {
        Personalizacion logo = personalizacionRepository.findLogo()
                .orElseThrow(() -> new PersonalizacionException("Logo no encontrado"));

        logo.setImagenUrl(null);
        return personalizacionRepository.save(logo);
    }

    // ===================== Slides =====================

    @Transactional(readOnly = true)
    public List<Personalizacion> listarSlides() {
        return personalizacionRepository.findAllSlides();
    }

    @Transactional(readOnly = true)
    public List<Personalizacion> listarSlidesConMarca() {
        return personalizacionRepository.findAllSlidesWithMarca();
    }

    @Transactional(readOnly = true)
    public Optional<Personalizacion> obtenerSlidePorOrden(Integer orden) {
        validarOrden(orden);
        return personalizacionRepository.findByTipoAndOrden(TIPO_SLIDE, orden);
    }

    /**
     * Actualiza/reemplaza un slide existente
     */
    @Transactional
    public Personalizacion actualizarSlide(Integer orden, String imagenUrl, Long marcaId) {
        validarOrden(orden);

        if (imagenUrl == null || imagenUrl.trim().isEmpty()) {
            throw new PersonalizacionException("La URL de la imagen no puede estar vacía");
        }

        Personalizacion slide = personalizacionRepository.findByTipoAndOrden(TIPO_SLIDE, orden)
                .orElseThrow(() -> new PersonalizacionException("Slide no encontrado en orden: " + orden));

        slide.setImagenUrl(imagenUrl.trim());

        // Asignar marca si se proporciona
        if (marcaId != null) {
            Brand marca = brandService.obtenerMarcaPorId(marcaId)
                    .orElseThrow(() -> new PersonalizacionException("Marca no encontrada con ID: " + marcaId));
            slide.setMarca(marca);
        } else {
            slide.setMarca(null);
        }

        return personalizacionRepository.save(slide);
    }

    /**
     * Actualiza solo la marca de un slide (sin cambiar la imagen)
     */
    @Transactional
    public Personalizacion actualizarMarcaSlide(Integer orden, Long marcaId) {
        validarOrden(orden);

        Personalizacion slide = personalizacionRepository.findByTipoAndOrden(TIPO_SLIDE, orden)
                .orElseThrow(() -> new PersonalizacionException("Slide no encontrado en orden: " + orden));

        if (marcaId != null) {
            Brand marca = brandService.obtenerMarcaPorId(marcaId)
                    .orElseThrow(() -> new PersonalizacionException("Marca no encontrada con ID: " + marcaId));
            slide.setMarca(marca);
        } else {
            slide.setMarca(null);
        }

        return personalizacionRepository.save(slide);
    }

    /**
     * Elimina la imagen y marca de un slide (lo deja vacío)
     */
    @Transactional
    public Personalizacion limpiarSlide(Integer orden) {
        validarOrden(orden);

        Personalizacion slide = personalizacionRepository.findByTipoAndOrden(TIPO_SLIDE, orden)
                .orElseThrow(() -> new PersonalizacionException("Slide no encontrado en orden: " + orden));

        slide.setImagenUrl(null);
        slide.setMarca(null);

        return personalizacionRepository.save(slide);
    }

    // ===================== Búsquedas y utilidades =====================

    @Transactional(readOnly = true)
    public List<Personalizacion> obtenerSlidesPorMarca(Long marcaId) {
        if (marcaId == null || marcaId <= 0) {
            throw new PersonalizacionException("ID de marca inválido");
        }
        return personalizacionRepository.findSlidesByMarcaId(marcaId);
    }

    @Transactional(readOnly = true)
    public List<Personalizacion> obtenerSlidesSinMarca() {
        return personalizacionRepository.findSlidesWithoutMarca();
    }

    @Transactional(readOnly = true)
    public Optional<String> obtenerImagenUrlLogo() {
        return obtenerLogo()
                .map(Personalizacion::getImagenUrl)
                .filter(url -> url != null && !url.trim().isEmpty());
    }

    @Transactional(readOnly = true)
    public boolean slideEstaVacio(Integer orden) {
        validarOrden(orden);
        return personalizacionRepository.findByTipoAndOrden(TIPO_SLIDE, orden)
                .map(slide -> slide.getImagenUrl() == null || slide.getImagenUrl().trim().isEmpty())
                .orElse(true);
    }

    @Transactional(readOnly = true)
    public boolean logoEstaVacio() {
        return obtenerLogo()
                .map(logo -> logo.getImagenUrl() == null || logo.getImagenUrl().trim().isEmpty())
                .orElse(true);
    }

    @Transactional(readOnly = true)
    public long contarSlidesConImagen() {
        return listarSlides().stream()
                .filter(slide -> slide.getImagenUrl() != null && !slide.getImagenUrl().trim().isEmpty())
                .count();
    }

    // ===================== Validaciones =====================

    private void validarOrden(Integer orden) {
        if (orden == null || orden < 1 || orden > MAX_SLIDES) {
            throw new PersonalizacionException("El orden debe estar entre 1 y " + MAX_SLIDES);
        }
    }

    // ===================== Excepción personalizada =====================

    public static class PersonalizacionException extends RuntimeException {
        public PersonalizacionException(String message) {
            super(message);
        }
        public PersonalizacionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}