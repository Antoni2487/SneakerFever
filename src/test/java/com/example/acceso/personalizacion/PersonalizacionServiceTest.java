package com.example.acceso.personalizacion;

import com.example.acceso.marca.Marca;
import com.example.acceso.marca.MarcaService;
import com.example.acceso.personalizacion.PersonalizacionService.PersonalizacionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PersonalizacionService - logo y slides del sitio")
class PersonalizacionServiceTest {

    @Mock
    private PersonalizacionRepository repository;

    @Mock
    private MarcaService marcaService;

    /** El constructor inicializa logo + slides: se crea después de preparar los mocks. */
    private PersonalizacionService crearService() {
        PersonalizacionService service = new PersonalizacionService(repository, marcaService);
        clearInvocations(repository);
        return service;
    }

    @Test
    void baseVacia_alArrancarCreaUnLogoYCincoSlides() {
        new PersonalizacionService(repository, marcaService);

        ArgumentCaptor<Personalizacion> creados = ArgumentCaptor.forClass(Personalizacion.class);
        verify(repository, times(6)).save(creados.capture());
        assertThat(creados.getAllValues()).filteredOn(p -> "LOGO".equals(p.getTipo())).hasSize(1);
        assertThat(creados.getAllValues()).filteredOn(p -> "SLIDE".equals(p.getTipo()))
                .extracting(Personalizacion::getOrden).containsExactly(1, 2, 3, 4, 5);
    }

    @Test
    void ordenFueraDeRango_seRechaza() {
        PersonalizacionService service = crearService();

        assertThatThrownBy(() -> service.actualizarSlide(6, "/uploads/personalizacion/x.png", null))
                .isInstanceOf(PersonalizacionException.class);
        assertThatThrownBy(() -> service.limpiarSlide(0)).isInstanceOf(PersonalizacionException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void actualizarSlide_asignaImagenYMarca() {
        PersonalizacionService service = crearService();
        Personalizacion slide = Personalizacion.builder().tipo("SLIDE").orden(2).build();
        Marca nike = Marca.builder().id(1L).nombre("Nike").build();
        when(repository.findByTipoAndOrden("SLIDE", 2)).thenReturn(Optional.of(slide));
        when(marcaService.obtenerMarcaPorId(1L)).thenReturn(Optional.of(nike));
        when(repository.save(slide)).thenReturn(slide);

        Personalizacion r = service.actualizarSlide(2, " /uploads/personalizacion/s2.png ", 1L);

        assertThat(r.getImagenUrl()).isEqualTo("/uploads/personalizacion/s2.png");
        assertThat(r.getMarca()).isEqualTo(nike);
    }

    @Test
    void actualizarSlide_marcaInexistente_seRechazaSinGuardar() {
        PersonalizacionService service = crearService();
        when(repository.findByTipoAndOrden("SLIDE", 1))
                .thenReturn(Optional.of(Personalizacion.builder().tipo("SLIDE").orden(1).build()));
        when(marcaService.obtenerMarcaPorId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.actualizarSlide(1, "/uploads/personalizacion/x.png", 99L))
                .isInstanceOf(PersonalizacionException.class).hasMessageContaining("Marca no encontrada");
        verify(repository, never()).save(any());
    }

    @Test
    void limpiarSlide_quitaImagenYMarca() {
        PersonalizacionService service = crearService();
        Personalizacion slide = Personalizacion.builder().tipo("SLIDE").orden(3)
                .imagenUrl("/uploads/personalizacion/s3.png").marca(Marca.builder().id(1L).build()).build();
        when(repository.findByTipoAndOrden("SLIDE", 3)).thenReturn(Optional.of(slide));
        when(repository.save(slide)).thenReturn(slide);

        service.limpiarSlide(3);

        assertThat(slide.getImagenUrl()).isNull();
        assertThat(slide.getMarca()).isNull();
    }

    @Test
    void actualizarLogo_urlVacia_seRechaza() {
        PersonalizacionService service = crearService();

        assertThatThrownBy(() -> service.actualizarLogo("  ")).isInstanceOf(PersonalizacionException.class);
        verify(repository, never()).save(any());
    }
}
