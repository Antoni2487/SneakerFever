// Define el paquete al que pertenece la clase.
package com.example.acceso.common;

// Importaciones para el manejo de logs y excepciones específicas.
import com.example.acceso.marca.MarcaService.MarcaException;
import com.example.acceso.categoria.CategoriaService.CategoriaException;
import com.example.acceso.cliente.ClienteService.ClienteException;
import com.example.acceso.personalizacion.PersonalizacionService.PersonalizacionException;
import com.example.acceso.producto.ProductoService.ProductoException;
import com.example.acceso.usuario.UsuarioService.AccesoDenegadoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Map;

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
    public ResponseEntity<Map<String, Object>> handleTypeMismatchException(TypeMismatchException ex) {
        // Registra una advertencia en la consola con detalles sobre el error.
        logger.warn("Se detectó un intento de acceder a una URL con un tipo de dato incorrecto. " +
                "Valor: '{}', Tipo requerido: '{}'.", ex.getValue(), ex.getRequiredType());
        return ApiResponses.error("Parámetro inválido en la URL", HttpStatus.BAD_REQUEST);
    }

    // Excepciones de negocio de los distintos dominios (Marca, Categoría,
    // Cliente, Producto, Personalización). Son errores de validación de negocio
    // (p. ej. nombre duplicado), no fallos del servidor, así que se devuelven
    // como 400 con el mismo formato JSON que ya usan los controllers.
    @ExceptionHandler({
            MarcaException.class,
            CategoriaException.class,
            ClienteException.class,
            ProductoException.class,
            PersonalizacionException.class
    })
    public ResponseEntity<Map<String, Object>> handleBusinessException(RuntimeException ex) {
        logger.warn("Excepción de negocio no controlada localmente: {}", ex.getMessage());
        // mensajeSeguro: algunas envuelven un error de BD con su texto
        // ("Error al guardar el producto: <SQL>", causa adjunta).
        return ApiResponses.error(ApiResponses.mensajeSeguro(ex), HttpStatus.BAD_REQUEST);
    }

    // Reglas de "último administrador" (UsuarioService): antes se atrapaba
    // este mismo tipo con try/catch repetido en cada método de
    // UsuarioController; centralizado acá como el resto de las excepciones
    // de negocio, solo que con 403 en vez de 400 porque es una denegación de
    // acceso, no un dato inválido.
    @ExceptionHandler(AccesoDenegadoException.class)
    public ResponseEntity<Map<String, Object>> handleAccesoDenegadoException(AccesoDenegadoException ex) {
        logger.warn("Acceso denegado: {}", ex.getMessage());
        return ApiResponses.error(ex.getMessage(), HttpStatus.FORBIDDEN);
    }

    // @PreAuthorize lanza AccessDeniedException DENTRO de la invocación del
    // controller, así que llega aquí y no al accessDeniedHandler de
    // SecurityConfig. Sin este handler la atrapaba el de Exception.class y
    // respondía 500 en vez de 403.
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDeniedException(AccessDeniedException ex) {
        return ApiResponses.error("No tienes permiso para realizar esta acción", HttpStatus.FORBIDDEN);
    }

    // Red de seguridad para cualquier excepción no controlada que llegue hasta
    // aquí sin pasar por un try/catch local del controller.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpectedException(Exception ex) {
        logger.error("Excepción no controlada llegó al GlobalExceptionHandler", ex);
        return ApiResponses.error("Ocurrió un error inesperado: " + ApiResponses.mensajeSeguro(ex),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }
}