package com.example.acceso.perfil;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PerfilService - perfiles y sus opciones")
class PerfilServiceTest {

    @Mock
    private PerfilRepository perfilRepository;

    @Mock
    private OpcionRepository opcionRepository;

    @InjectMocks
    private PerfilService perfilService;

    private PerfilRequest request(Long id, Set<Long> opciones) {
        PerfilRequest r = new PerfilRequest();
        r.setId(id);
        r.setNombre("Vendedor");
        r.setDescripcion("Ventas");
        r.setOpciones(opciones);
        return r;
    }

    @Test
    void guardarNuevo_asignaLasOpcionesPedidas() {
        Opcion ventas = Opcion.builder().id(7L).nombre("Ventas").ruta("/ventas").permiso("VENTAS").build();
        when(opcionRepository.findAllById(Set.of(7L))).thenReturn(List.of(ventas));
        when(perfilRepository.save(any(Perfil.class))).thenAnswer(inv -> inv.getArgument(0));

        Perfil guardado = perfilService.guardarPerfil(request(null, Set.of(7L)));

        assertThat(guardado.getNombre()).isEqualTo("Vendedor");
        assertThat(guardado.getOpciones()).containsExactly(ventas);
    }

    @Test
    void editarSinListaDeOpciones_conservaLasQueTenia() {
        Opcion dashboard = Opcion.builder().id(1L).nombre("Dashboard").build();
        Perfil existente = Perfil.builder().id(3L).nombre("Viejo").estado(true)
                .opciones(new HashSet<>(Set.of(dashboard))).build();
        when(perfilRepository.findById(3L)).thenReturn(Optional.of(existente));
        when(perfilRepository.save(any(Perfil.class))).thenAnswer(inv -> inv.getArgument(0));

        Perfil guardado = perfilService.guardarPerfil(request(3L, null));

        assertThat(guardado.getNombre()).isEqualTo("Vendedor");
        assertThat(guardado.getOpciones()).containsExactly(dashboard);
        verify(opcionRepository, never()).findAllById(any());
    }

    @Test
    void cambiarEstado_alternaActivoInactivo() {
        Perfil perfil = Perfil.builder().id(2L).nombre("Editor").estado(true).build();
        when(perfilRepository.findById(2L)).thenReturn(Optional.of(perfil));
        when(perfilRepository.save(perfil)).thenReturn(perfil);

        assertThat(perfilService.cambiarEstadoPerfil(2L)).get().extracting(Perfil::isEstado).isEqualTo(false);
    }

    @Test
    void cambiarEstado_perfilInexistente_vacio() {
        when(perfilRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(perfilService.cambiarEstadoPerfil(99L)).isEmpty();
        verify(perfilRepository, never()).save(any());
    }
}
