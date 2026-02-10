package com.example.acceso.config;

import com.example.acceso.model.Personalizacion;
import com.example.acceso.service.PersonalizacionService;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;
import java.util.Optional;

@ControllerAdvice
@Component
public class GlobalPersonalizacionAttributes {  // ← DEBE LLAMARSE ASÍ, NO "GlobalModelAttributes"

    private final PersonalizacionService personalizacionService;

    public GlobalPersonalizacionAttributes(PersonalizacionService personalizacionService) {
        this.personalizacionService = personalizacionService;
    }

    @ModelAttribute
    public void addGlobalAttributes(Model model) {
        // Logo del sitio
        Optional<String> logoUrl = personalizacionService.obtenerImagenUrlLogo();
        model.addAttribute("siteLogo", logoUrl.orElse("/web/images/logo.png"));

        // Slides del carrusel (solo los que tienen imagen)
        List<Personalizacion> slides = personalizacionService.listarSlidesConMarca()
            .stream()
            .filter(s -> s.getImagenUrl() != null && !s.getImagenUrl().trim().isEmpty())
            .toList();
        model.addAttribute("siteSlides", slides);
    }
}