package com.example.acceso.seguridad;

import java.util.Locale;
import java.util.Set;

/**
 * Códigos de permiso: el valor de {@code opciones.permiso} en BD y la
 * autoridad que exige cada controller con
 * {@code @PreAuthorize("hasAuthority('...')")}.
 *
 * Son estables a propósito: la ruta de una Opción es navegación (el link del
 * sidebar) y puede cambiar; el permiso no. Antes la autoridad se derivaba de
 * la ruta, así que renombrar un link rompía los permisos sin aviso.
 */
public final class Permisos {

    public static final String DASHBOARD = "DASHBOARD";
    public static final String PRODUCTOS = "PRODUCTOS";
    public static final String CATEGORIAS = "CATEGORIAS";
    public static final String MARCAS = "MARCAS";
    public static final String VENTAS = "VENTAS";
    public static final String INVENTARIO = "INVENTARIO";
    public static final String CLIENTES = "CLIENTES";
    public static final String USUARIOS = "USUARIOS";
    public static final String PERFILES = "PERFILES";
    public static final String PERSONALIZACION = "PERSONALIZACION";

    public static final Set<String> TODOS = Set.of(
            DASHBOARD, PRODUCTOS, CATEGORIAS, MARCAS, VENTAS, INVENTARIO,
            CLIENTES, USUARIOS, PERFILES, PERSONALIZACION);

    private Permisos() {
    }

    /**
     * Permiso para {@code /api/upload/{tipo}/**}: cada carpeta de imágenes la
     * gestiona el dominio del mismo nombre ("productos" → PRODUCTOS). Un tipo
     * desconocido devuelve un código que nadie tiene, así que se deniega.
     */
    public static String deTipoUpload(String tipo) {
        return tipo == null ? "" : tipo.toUpperCase(Locale.ROOT);
    }

    /**
     * Deduce el permiso de una Opción a partir de su ruta, como hacía la versión
     * anterior del filtro JWT. Solo lo usa {@link PermisosOpcionInitializer} para
     * completar {@code opciones.permiso} en bases creadas antes de esta columna;
     * en tiempo de petición el permiso se lee siempre de la columna.
     *
     * @return el código, o {@code null} si la ruta no corresponde a ningún permiso conocido
     */
    public static String deducirDeRuta(String ruta) {
        if (ruta == null || ruta.isBlank()) {
            return null;
        }
        String segmento = "";
        for (String parte : ruta.split("/")) {
            if (!parte.isBlank()) {
                segmento = parte.toUpperCase(Locale.ROOT);
                break;
            }
        }
        // El dashboard nunca siguió la convención "ruta = dominio": en acceso.sql
        // histórico es "/", el dashboard antiguo vivía en "/admin" y el de React
        // en "/dashboard".
        if (segmento.isEmpty() || segmento.equals("ADMIN")) {
            return DASHBOARD;
        }
        return TODOS.contains(segmento) ? segmento : null;
    }
}
