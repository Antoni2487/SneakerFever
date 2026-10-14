package com.example.acceso.marca;

import com.example.acceso.marca.MarcaService.MarcaException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MarcaService - alta, edición, imagen y borrado lógico")
class MarcaServiceTest {

    @Mock
    private MarcaRepository brandRepository;

    @InjectMocks
    private MarcaService marcaService;

    @Test
    void crear_normalizaTextoYQuedaActiva() {
        when(brandRepository.save(any(Marca.class))).thenAnswer(inv -> inv.getArgument(0));
        Marca nueva = Marca.builder().nombre("  Nike ").descripcion("   ").estado(0).build();

        Marca guardada = marcaService.guardarMarca(nueva);

        assertThat(guardada.getNombre()).isEqualTo("Nike");
        assertThat(guardada.getDescripcion()).as("descripción en blanco se guarda como null").isNull();
        assertThat(guardada.getEstado()).as("una marca nueva siempre nace activa").isEqualTo(1);
    }

    @Test
    void editarSinImagen_conservaLaImagenYEstadoAnteriores() {
        Marca existente = Marca.builder().id(3L).nombre("Nike").estado(0).imagen("/uploads/marcas/nike.png").build();
        when(brandRepository.findById(3L)).thenReturn(Optional.of(existente));
        when(brandRepository.save(any(Marca.class))).thenAnswer(inv -> inv.getArgument(0));
        Marca cambios = Marca.builder().id(3L).nombre("Nike Inc").build();
        cambios.setEstado(null);

        Marca guardada = marcaService.guardarMarca(cambios);

        assertThat(guardada.getImagen()).isEqualTo("/uploads/marcas/nike.png");
        assertThat(guardada.getEstado()).isEqualTo(0);
    }

    @Test
    void nombreVacio_seRechazaSinGuardar() {
        assertThatThrownBy(() -> marcaService.guardarMarca(Marca.builder().nombre("  ").build()))
                .isInstanceOf(MarcaException.class).hasMessageContaining("obligatorio");
        verify(brandRepository, never()).save(any());
    }

    @Test
    void eliminar_esBorradoLogicoConEstado2() {
        Marca marca = Marca.builder().id(3L).nombre("Nike").estado(1).build();
        when(brandRepository.findById(3L)).thenReturn(Optional.of(marca));

        marcaService.eliminarMarca(3L);

        assertThat(marca.getEstado()).isEqualTo(2);
        verify(brandRepository).save(marca);
    }

    @Test
    void actualizarImagen_urlVacia_seRechaza() {
        assertThatThrownBy(() -> marcaService.actualizarImagen(3L, " "))
                .isInstanceOf(MarcaException.class).hasMessageContaining("no puede estar vacía");
        verify(brandRepository, never()).save(any());
    }

    @Test
    void existeMarca_ignoraLasEliminadasYElTextoVacio() {
        when(brandRepository.existsByNombreAndEstadoNot("Nike", 2)).thenReturn(true);

        assertThat(marcaService.existeMarca(" Nike ")).isTrue();
        assertThat(marcaService.existeMarca("  ")).isFalse();
    }
}
