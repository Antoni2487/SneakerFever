// Define el paquete al que pertenece la clase.
package com.example.acceso.controller;

// Importaciones para el manejo de logs y excepciones específicas.
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

// @ControllerAdvice: Convierte esta clase en un componente global que puede manejar
// excepciones de todos los controladores de la aplicación.
@ControllerAdvice
public class GlobalExceptionHandler {

    // Inicializa un logger para registrar información útil cuando ocurra una
    // excepción.
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // @ExceptionHandler: Le dice a Spring que este método debe ejecutarse cada vez
    // que se produzca una excepción de tipo TypeMismatchException en cualquier
    // controlador.
    // Esto ocurre, por ejemplo, si alguien intenta acceder a /usuarios/api/abc en
    // lugar de /usuarios/api/123.
    @ExceptionHandler(TypeMismatchException.class)
    public String handleTypeMismatchException(TypeMismatchException ex) {
        // Registra una advertencia en la consola con detalles sobre el error.
        // Es una buena práctica para saber qué tipo de errores están ocurriendo.
        logger.warn("Se detectó un intento de acceder a una URL con un tipo de dato incorrecto. " +
                "Valor: '{}', Tipo requerido: '{}'. Redirigiendo a la página de inicio.",
                ex.getValue(), ex.getRequiredType());
        // En lugar de mostrar una página de error genérica, redirige al usuario a la
        // página
        // de inicio, lo cual es una experiencia de usuario más amigable.
        return "redirect:/";
    }
}