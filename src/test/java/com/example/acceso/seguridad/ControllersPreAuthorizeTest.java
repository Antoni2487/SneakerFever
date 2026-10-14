package com.example.acceso.seguridad;

import com.example.acceso.carrito.CartController;
import com.example.acceso.carrito.CheckoutController;
import com.example.acceso.common.PublicSiteController;
import com.example.acceso.common.WebController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Red de seguridad del RBAC por método: SecurityConfig solo exige estar
 * autenticado en las APIs del panel; el permiso real lo pone @PreAuthorize en
 * cada controller. Si alguien agrega un controller o un endpoint sin él,
 * cualquier usuario logueado podría usarlo — este test lo detecta.
 */
@DisplayName("Todo endpoint del panel declara @PreAuthorize")
class ControllersPreAuthorizeTest {

    /** Controllers públicos o con su propia validación, a propósito sin @PreAuthorize. */
    private static final Set<Class<?>> SIN_PERMISO_POR_DISENO = Set.of(
            AuthController.class,       // login / refresh / me
            LoginController.class,      // login Thymeleaf legacy
            WebController.class,        // sitio público Thymeleaf
            PublicSiteController.class, // /api/public/**
            CartController.class,       // carrito público; checkout exige JWT en SecurityConfig
            CheckoutController.class);  // checkout legacy, valida su propia sesión

    @Test
    void todoEndpointDeControllerNoPublicoTienePreAuthorize() throws ClassNotFoundException {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Controller.class)); // incluye @RestController

        List<String> sinPermiso = new ArrayList<>();
        int controllersRevisados = 0;
        for (BeanDefinition bd : scanner.findCandidateComponents("com.example.acceso")) {
            Class<?> controller = Class.forName(bd.getBeanClassName());
            if (SIN_PERMISO_POR_DISENO.contains(controller)) {
                continue;
            }
            controllersRevisados++;
            boolean claseProtegida = AnnotatedElementUtils.hasAnnotation(controller, PreAuthorize.class);
            for (Method metodo : controller.getDeclaredMethods()) {
                if (AnnotatedElementUtils.hasAnnotation(metodo, RequestMapping.class)
                        && !claseProtegida
                        && !AnnotatedElementUtils.hasAnnotation(metodo, PreAuthorize.class)) {
                    sinPermiso.add(controller.getSimpleName() + "." + metodo.getName());
                }
            }
        }

        assertThat(controllersRevisados).as("controllers del panel encontrados").isGreaterThanOrEqualTo(12);
        assertThat(sinPermiso).as("endpoints sin @PreAuthorize").isEmpty();
    }
}
