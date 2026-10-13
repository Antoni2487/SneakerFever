package com.example.acceso.seguridad;

import com.example.acceso.perfil.Opcion;
import com.example.acceso.perfil.OpcionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PermisosOpcionInitializer - completa opciones.permiso en bases existentes")
class PermisosOpcionInitializerTest {

    @Mock
    private OpcionRepository opcionRepository;

    @InjectMocks
    private PermisosOpcionInitializer initializer;

    @Test
    void completaElPermisoDeducidoDeLaRuta() {
        Opcion productos = Opcion.builder().id(4L).nombre("Productos").ruta("/productos").build();
        Opcion dashboard = Opcion.builder().id(1L).nombre("Dashboard").ruta("/").build();
        when(opcionRepository.findByPermisoIsNull()).thenReturn(List.of(productos, dashboard));

        initializer.run(null);

        assertThat(productos.getPermiso()).isEqualTo(Permisos.PRODUCTOS);
        assertThat(dashboard.getPermiso()).isEqualTo(Permisos.DASHBOARD);
        verify(opcionRepository).save(productos);
        verify(opcionRepository).save(dashboard);
    }

    @Test
    void rutaSinPermisoConocidoQuedaVacia() {
        Opcion reportes = Opcion.builder().id(20L).nombre("Reportes").ruta("/reportes").build();
        when(opcionRepository.findByPermisoIsNull()).thenReturn(List.of(reportes));

        initializer.run(null);

        assertThat(reportes.getPermiso()).isNull();
        verify(opcionRepository, never()).save(reportes);
    }
}
