package com.example.acceso.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class ApiResponses {

    private static final Logger log = LoggerFactory.getLogger(ApiResponses.class);

    public static final String MENSAJE_ERROR_INTERNO = "Error interno del servidor";

    /**
     * Tipos cuyo mensaje lo escribe nuestro propio código para el usuario
     * ("Stock insuficiente", "La venta ya está anulada"...). Clase EXACTA, no
     * subclases: las excepciones de Hibernate/Spring/JDBC heredan de
     * RuntimeException y su mensaje trae SQL, nombres de tablas o URLs internas.
     */
    private static final Set<Class<?>> EXCEPCIONES_DE_NEGOCIO = Set.of(
            RuntimeException.class, IllegalArgumentException.class, IllegalStateException.class);

    private ApiResponses() {
    }

    /**
     * El mensaje de {@code e} si es seguro mostrarlo al cliente; si no,
     * {@link #MENSAJE_ERROR_INTERNO}, y el detalle real queda en el log.
     *
     * Seguro = toda la cadena de causas son excepciones de negocio: los tipos de
     * {@link #EXCEPCIONES_DE_NEGOCIO} o las propias de este proyecto (p. ej.
     * {@code ProductoService.ProductoException}). Un RuntimeException que envuelve
     * un error de BD o de RestTemplate tampoco se muestra.
     */
    public static String mensajeSeguro(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (!esDeNegocio(t)) {
                log.error("Error interno (mensaje ocultado al cliente)", e);
                return MENSAJE_ERROR_INTERNO;
            }
            if (t.getCause() == t) {
                break;
            }
        }
        return e.getMessage() != null ? e.getMessage() : MENSAJE_ERROR_INTERNO;
    }

    private static boolean esDeNegocio(Throwable t) {
        return EXCEPCIONES_DE_NEGOCIO.contains(t.getClass())
                || t.getClass().getName().startsWith("com.example.acceso.");
    }

    public static ResponseEntity<Map<String, Object>> error(String message) {
        return error(message, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public static ResponseEntity<Map<String, Object>> error(String message, HttpStatus status) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        response.put("timestamp", Instant.now().toString());
        response.put("path", currentPath());
        return ResponseEntity.status(status).body(response);
    }

    public static ResponseEntity<Map<String, Object>> validationError(BindingResult result) {
        Map<String, String> errors = new HashMap<>();
        result.getFieldErrors().forEach(fieldError ->
                errors.put(fieldError.getField(), fieldError.getDefaultMessage()));

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "Errores de validación");
        response.put("errors", errors);
        response.put("timestamp", Instant.now().toString());
        response.put("path", currentPath());
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * La ruta real de la petición actual, para el campo "path" del error.
     * {@code ApiResponses} se llama desde controllers y desde
     * {@code GlobalExceptionHandler}, así que en vez de agregar un parámetro
     * a cada llamada existente, se lee del {@code HttpServletRequest} de la
     * petición en curso (siempre hay una en un método @Controller/@ExceptionHandler).
     */
    private static String currentPath() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            return attrs.getRequest().getRequestURI();
        }
        return null;
    }
}
