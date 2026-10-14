package com.example.acceso.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.lang.NonNull;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;


@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${upload.path:src/main/resources/static/uploads}")
    private String uploadPath;

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
            .requestFactory(() -> {
                HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
                factory.setConnectTimeout(10_000);
                factory.setReadTimeout(10_000);
                return factory;
            })
            .build();
    }


    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        // Imágenes subidas por los usuarios (productos, marcas, personalización).
        // Los assets del sitio (static/web/images) los sirve el handler por defecto de Spring.
        Path uploadDir = Paths.get(uploadPath).toAbsolutePath().normalize();
        String uploadLocation = "file:///" + uploadDir.toString().replace("\\", "/") + "/";

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadLocation)
                .setCachePeriod(3600); // Cache de 1 hora para imágenes de productos
    }

    // La protección de rutas y el CORS viven en SecurityConfig.
}