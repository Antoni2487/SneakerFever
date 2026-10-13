package com.example.acceso.seguridad;

import com.example.acceso.perfil.Opcion;
import com.example.acceso.perfil.OpcionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Migración de datos idempotente para {@code opciones.permiso}: al arrancar,
 * completa las opciones que aún no tienen permiso deduciéndolo de su ruta
 * ({@link Permisos#deducirDeRuta}). Así una base creada antes de esta columna
 * sigue funcionando sin correr scripts a mano — Hibernate agrega la columna
 * vacía ({@code ddl-auto=update}) y esto la llena.
 *
 * Solo toca filas con {@code permiso IS NULL}: un permiso asignado a mano en BD
 * nunca se sobrescribe. Una ruta que no corresponde a ningún permiso se deja
 * vacía y se avisa en el log — esa opción no otorga acceso a ninguna API.
 */
@Component
public class PermisosOpcionInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PermisosOpcionInitializer.class);

    private final OpcionRepository opcionRepository;

    public PermisosOpcionInitializer(OpcionRepository opcionRepository) {
        this.opcionRepository = opcionRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<Opcion> sinPermiso = opcionRepository.findByPermisoIsNull();
        for (Opcion opcion : sinPermiso) {
            String permiso = Permisos.deducirDeRuta(opcion.getRuta());
            if (permiso == null) {
                log.warn("Opción '{}' (ruta '{}') sin permiso: no corresponde a ningún código de Permisos. "
                        + "Asígnalo a mano en opciones.permiso si debe dar acceso a una API.",
                        opcion.getNombre(), opcion.getRuta());
                continue;
            }
            opcion.setPermiso(permiso);
            opcionRepository.save(opcion);
            log.info("Opción '{}' (ruta '{}'): permiso completado a {}", opcion.getNombre(), opcion.getRuta(), permiso);
        }
    }
}
