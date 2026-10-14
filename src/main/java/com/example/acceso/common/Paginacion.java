package com.example.acceso.common;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

/**
 * Paginación de los listados del panel ({@code GET /xxx/api/pagina}).
 *
 * Respuesta: {@code {success, data, page, size, totalElements, totalPages}},
 * con {@code page} empezando en 0.
 */
public final class Paginacion {

    public static final int TAMANO_POR_DEFECTO = 10;
    /** Tope de {@code size}: evita que un cliente pida "toda la tabla" de una vez. */
    public static final int TAMANO_MAXIMO = 100;

    private Paginacion() {
    }

    public static Pageable crear(int page, int size, Sort orden) {
        int pagina = Math.max(page, 0);
        int tamano = Math.min(Math.max(size, 1), TAMANO_MAXIMO);
        return PageRequest.of(pagina, tamano, orden);
    }

    /** Texto de búsqueda normalizado: {@code null} o en blanco = sin filtro (""). */
    public static String busqueda(String q) {
        return q == null ? "" : q.trim();
    }

    public static ResponseEntity<Map<String, Object>> respuesta(Page<?> pagina) {
        Map<String, Object> body = new HashMap<>();
        body.put("success", true);
        body.put("data", pagina.getContent());
        body.put("page", pagina.getNumber());
        body.put("size", pagina.getSize());
        body.put("totalElements", pagina.getTotalElements());
        body.put("totalPages", pagina.getTotalPages());
        return ResponseEntity.ok(body);
    }
}
