package com.example.acceso.seguridad;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Permisos - códigos y deducción desde rutas legacy")
class PermisosTest {

    @ParameterizedTest(name = "ruta \"{0}\" -> {1}")
    @CsvSource({
            "/productos, PRODUCTOS",
            "/usuarios/listar, USUARIOS",
            "/perfiles/listar, PERFILES",
            "/Ventas, VENTAS",
            "/, DASHBOARD",
            "/admin, DASHBOARD",
            "/dashboard, DASHBOARD",
    })
    void deduceElPermisoDeRutasConocidas(String ruta, String esperado) {
        assertThat(Permisos.deducirDeRuta(ruta)).isEqualTo(esperado);
    }

    @Test
    void rutaDesconocidaOVaciaNoDaPermiso() {
        assertThat(Permisos.deducirDeRuta("/reportes")).isNull();
        assertThat(Permisos.deducirDeRuta("")).isNull();
        assertThat(Permisos.deducirDeRuta(null)).isNull();
    }

    @Test
    void tipoDeUploadSeMapeaAlPermisoDelDominio() {
        assertThat(Permisos.deTipoUpload("productos")).isEqualTo(Permisos.PRODUCTOS);
        assertThat(Permisos.deTipoUpload("personalizacion")).isEqualTo(Permisos.PERSONALIZACION);
        assertThat(Permisos.TODOS).doesNotContain(Permisos.deTipoUpload("otra-cosa"));
    }
}
