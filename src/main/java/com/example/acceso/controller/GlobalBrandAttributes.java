package com.example.acceso.controller;

import com.example.acceso.model.Brand;
import com.example.acceso.service.BrandService;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import java.util.List;
import java.util.Optional;

@ControllerAdvice("brandAdvice")
public class GlobalBrandAttributes {

    private final BrandService brandService;

    public GlobalBrandAttributes(BrandService brandService) {
        this.brandService = brandService;
    }

    /**
     * Agrega las marcas a TODAS las vistas automáticamente
     */
    @ModelAttribute("marcas")
    public List<Brand> getMarcas() {
        List<Brand> marcas = Optional.ofNullable(
                brandService.listarMarcasConImagen()
        ).orElse(List.of());

        marcas.forEach(marca -> {
            if (marca.getImagen() != null) {
                String imagenLimpia = marca.getImagen()
                        .replace(" ", "")
                        .replace("up cargas", "uploads");

                if (!imagenLimpia.startsWith("/")) {
                    imagenLimpia = "/" + imagenLimpia;
                }

                marca.setImagen(imagenLimpia);
            }
        });

        return marcas;
    }
}
