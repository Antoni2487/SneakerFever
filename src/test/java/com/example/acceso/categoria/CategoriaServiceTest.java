package com.example.acceso.categoria;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
@DisplayName("CategoriaService - tests unitarios con Mockito")
class CategoryServiceTest {

    @Mock
    private CategoriaRepository categoryRepository;

    @InjectMocks
    private CategoriaService categoryService;

    // ===================== Crear =====================

    @Test
    @DisplayName("Crear categoría válida: la guarda con estado activo y normaliza el texto")
    void crearCategoriaValida_laGuardaConEstadoActivo() {
        Categoria nueva = Categoria.builder()
                .nombre("  Zapatillas Running  ")
                .descripcion("  Calzado deportivo  ")
                .build();
        when(categoryRepository.save(any(Categoria.class))).thenAnswer(inv -> inv.getArgument(0));

        Categoria guardada = categoryService.guardarCategoria(nueva);

        ArgumentCaptor<Categoria> captor = ArgumentCaptor.forClass(Categoria.class);
        verify(categoryRepository).save(captor.capture());
        Categoria enviadaAlRepositorio = captor.getValue();

        assertThat(enviadaAlRepositorio.getNombre()).isEqualTo("Zapatillas Running");
        assertThat(enviadaAlRepositorio.getDescripcion()).isEqualTo("Calzado deportivo");
        assertThat(enviadaAlRepositorio.getEstado()).isEqualTo(1);
        assertThat(guardada).isSameAs(enviadaAlRepositorio);
    }

    @Test
    @DisplayName("Crear categoría sin nombre: lanza CategoriaException y no toca el repositorio")
    void crearCategoriaSinNombre_lanzaCategoriaException() {
        Categoria invalida = Categoria.builder().nombre("   ").build();

        assertThatThrownBy(() -> categoryService.guardarCategoria(invalida))
                .isInstanceOf(CategoriaService.CategoriaException.class)
                .hasMessageContaining("obligatorio");

        verify(categoryRepository, never()).save(any(Categoria.class));
    }

    // ===================== Obtener por ID =====================

    @Test
    @DisplayName("Obtener por ID existente: devuelve la categoría")
    void obtenerCategoriaPorIdExistente_laDevuelve() {
        Categoria existente = Categoria.builder().id(7L).nombre("Casual").estado(1).build();
        when(categoryRepository.findById(7L)).thenReturn(Optional.of(existente));

        Optional<Categoria> resultado = categoryService.obtenerCategoriaPorId(7L);

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getId()).isEqualTo(7L);
        assertThat(resultado.get().getNombre()).isEqualTo("Casual");
        verify(categoryRepository).findById(7L);
    }

    /**
     * OJO: {@code obtenerCategoriaPorId} NO lanza excepción de negocio ante un ID
     * inexistente — devuelve {@link Optional#empty()}. Este test fija ese contrato
     * real. La excepción de negocio aparece en las operaciones que sí exigen que la
     * categoría exista (ver los dos tests siguientes).
     */
    @Test
    @DisplayName("Obtener por ID inexistente: devuelve Optional vacío (no lanza excepción)")
    void obtenerCategoriaPorIdInexistente_devuelveOptionalVacio() {
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<Categoria> resultado = categoryService.obtenerCategoriaPorId(999L);

        assertThat(resultado).isEmpty();
    }

    @Test
    @DisplayName("Obtener por ID inválido (nulo o <= 0): corta antes de consultar al repositorio")
    void obtenerCategoriaPorIdInvalido_noConsultaAlRepositorio() {
        assertThat(categoryService.obtenerCategoriaPorId(null)).isEmpty();
        assertThat(categoryService.obtenerCategoriaPorId(0L)).isEmpty();
        assertThat(categoryService.obtenerCategoriaPorId(-3L)).isEmpty();

        verify(categoryRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Actualizar una categoría inexistente: lanza CategoriaException y no guarda")
    void actualizarCategoriaInexistente_lanzaCategoriaException() {
        Categoria fantasma = Categoria.builder().id(404L).nombre("Fantasma").build();
        when(categoryRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.guardarCategoria(fantasma))
                .isInstanceOf(CategoriaService.CategoriaException.class)
                .hasMessageContaining("no encontrada");

        verify(categoryRepository, never()).save(any(Categoria.class));
    }

    @Test
    @DisplayName("Actualizar sin enviar estado: conserva el estado que ya tenía la categoría")
    void actualizarSinEstado_conservaElEstadoExistente() {
        Categoria existente = Categoria.builder().id(12L).nombre("Urbano").estado(0).build();
        Categoria cambios = Categoria.builder().id(12L).nombre("Urbano Premium").build();
        when(categoryRepository.findById(12L)).thenReturn(Optional.of(existente));
        when(categoryRepository.save(any(Categoria.class))).thenAnswer(inv -> inv.getArgument(0));

        categoryService.guardarCategoria(cambios);

        ArgumentCaptor<Categoria> captor = ArgumentCaptor.forClass(Categoria.class);
        verify(categoryRepository).save(captor.capture());
        assertThat(captor.getValue().getEstado()).isEqualTo(0);
        assertThat(captor.getValue().getNombre()).isEqualTo("Urbano Premium");
    }

    // ===================== Eliminar =====================

    @Test
    @DisplayName("Eliminar categoría: borrado lógico con estado 2 y save en el repositorio")
    void eliminarCategoria_realizaBorradoLogicoConEstado2() {
        Categoria existente = Categoria.builder().id(5L).nombre("Outlet").estado(1).build();
        when(categoryRepository.findById(5L)).thenReturn(Optional.of(existente));

        categoryService.eliminarCategoria(5L);

        ArgumentCaptor<Categoria> captor = ArgumentCaptor.forClass(Categoria.class);
        verify(categoryRepository).findById(5L);
        verify(categoryRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(5L);
        assertThat(captor.getValue().getEstado()).isEqualTo(2);
    }

    @Test
    @DisplayName("Eliminar categoría inexistente: lanza CategoriaException y no guarda nada")
    void eliminarCategoriaInexistente_lanzaCategoriaException() {
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.eliminarCategoria(999L))
                .isInstanceOf(CategoriaService.CategoriaException.class)
                .hasMessageContaining("no encontrada");

        verify(categoryRepository, never()).save(any(Categoria.class));
    }

    // ===================== Cambiar estado =====================

    @Test
    @DisplayName("Cambiar estado de una categoría activa: pasa a inactiva (1 -> 0)")
    void cambiarEstadoCategoriaActiva_laDesactiva() {
        Categoria activa = Categoria.builder().id(3L).nombre("Sandalias").estado(1).build();
        when(categoryRepository.findById(3L)).thenReturn(Optional.of(activa));
        when(categoryRepository.save(any(Categoria.class))).thenAnswer(inv -> inv.getArgument(0));

        Optional<Categoria> resultado = categoryService.cambiarEstadoCategoria(3L);

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getEstado()).isEqualTo(0);
        verify(categoryRepository).save(activa);
    }

    @Test
    @DisplayName("Cambiar estado de una categoría inactiva: pasa a activa (0 -> 1)")
    void cambiarEstadoCategoriaInactiva_laActiva() {
        Categoria inactiva = Categoria.builder().id(4L).nombre("Botines").estado(0).build();
        when(categoryRepository.findById(4L)).thenReturn(Optional.of(inactiva));
        when(categoryRepository.save(any(Categoria.class))).thenAnswer(inv -> inv.getArgument(0));

        Optional<Categoria> resultado = categoryService.cambiarEstadoCategoria(4L);

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getEstado()).isEqualTo(1);
        verify(categoryRepository).save(inactiva);
    }

    @Test
    @DisplayName("Cambiar estado de una categoría inexistente: devuelve Optional vacío y no guarda")
    void cambiarEstadoCategoriaInexistente_devuelveOptionalVacio() {
        when(categoryRepository.findById(777L)).thenReturn(Optional.empty());

        Optional<Categoria> resultado = categoryService.cambiarEstadoCategoria(777L);

        assertThat(resultado).isEmpty();
        verify(categoryRepository, never()).save(any(Categoria.class));
    }
}
