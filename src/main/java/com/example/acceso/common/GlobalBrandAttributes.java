package com.example.acceso.common;

import com.example.acceso.marca.Marca;
import com.example.acceso.marca.MarcaService;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import java.util.List;
import java.util.Optional;

@ControllerAdvice("brandAdvice")
public class GlobalBrandAttributes {

    private final MarcaService brandService;

    public GlobalBrandAttributes(MarcaService brandService) {
        this.brandService = brandService;
    }

    /**
     * Agrega las marcas a TODAS las vistas automáticamente
     */
    @ModelAttribute("marcas")
    public List<Marca> getMarcas() {
        List<Marca> marcas = Optional.ofNullable(
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
