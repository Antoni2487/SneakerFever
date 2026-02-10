package com.example.acceso.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.lang.NonNull;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;


@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final SessionInterceptor sessionInterceptor;

    @Value("${upload.path:src/main/resources/static/uploads}")
    private String uploadPath;

    public WebConfig(SessionInterceptor sessionInterceptor) {
        this.sessionInterceptor = sessionInterceptor;
    }

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
        // CSS
        registry.addResourceHandler("/css/**")
                .addResourceLocations("classpath:/static/admin/css/", "classpath:/static/web/css/")
                .setCachePeriod(0);

        // JavaScript
        registry.addResourceHandler("/js/**")
                .addResourceLocations("classpath:/static/admin/js/", "classpath:/static/web/js/")
                .setCachePeriod(0);

        // Imágenes estáticas
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/admin/images/", "classpath:/static/web/images/")
                .setCachePeriod(0);

        // NUEVO: Imágenes subidas por los usuarios (productos)
        Path uploadDir = Paths.get(uploadPath).toAbsolutePath().normalize();
        String uploadLocation = "file:///" + uploadDir.toString().replace("\\", "/") + "/";

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadLocation)
                .setCachePeriod(3600); // Cache de 1 hora para imágenes de productos
    }

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        // Registra nuestro SessionInterceptor
        registry.addInterceptor(sessionInterceptor)
                .addPathPatterns("/admin/**")
                .excludePathPatterns(
                    "/login",
                    "/logout",
                    "/css/**",
                    "/js/**",
                    "/images/**",
                    "/uploads/**",  // NUEVO: Excluir uploads del interceptor
                    "/error",
                    "/favicon.ico"
                );
    }

    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        // Configuración CORS para APIs
        registry.addMapping("/usuarios/api/**")
                .allowedOrigins("http://localhost:8080")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);

        // NUEVO: CORS para API de upload
        registry.addMapping("/api/upload/**")
                .allowedOrigins("http://localhost:8080")
                .allowedMethods("GET", "POST", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}