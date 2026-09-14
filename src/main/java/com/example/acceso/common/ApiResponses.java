package com.example.acceso.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public final class ApiResponses {

    private ApiResponses() {
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
