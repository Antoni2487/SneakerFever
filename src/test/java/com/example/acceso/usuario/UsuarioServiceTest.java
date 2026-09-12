package com.example.acceso.usuario;

import com.example.acceso.perfil.Perfil;
import com.example.acceso.perfil.PerfilRepository;
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
@DisplayName("UsuarioService - tests unitarios con Mockito")
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PerfilRepository perfilRepository;

    @InjectMocks
    private UsuarioService usuarioService;

    // ===================== Fixtures =====================

    private Perfil perfil(Long id, String nombre) {
        return Perfil.builder().id(id).nombre(nombre).descripcion("").estado(true).build();
    }

    private Usuario usuarioExistente(Long id, Integer estado, Perfil perfil, String clave) {
        return Usuario.builder()
                .id(id)
                .nombre("Original")
                .usuario("original")
                .correo("original@sneakerfever.test")
                .clave(clave)
                .estado(estado)
                .perfil(perfil)
                .build();
    }

    private UsuarioRequest request(Long id, String clave, Boolean estado) {
        UsuarioRequest r = new UsuarioRequest();
        r.setId(id);
        r.setNombre("Nuevo Nombre");
        r.setUsuario("nuevousuario");
        r.setCorreo("nuevo@sneakerfever.test");
        r.setClave(clave);
        r.setEstado(estado);
        return r;
    }

    // ===================== guardarUsuario(UsuarioRequest) - estado =====================

    @Test
    @DisplayName("Actualizar sin enviar estado: conserva el estado existente")
    void actualizarSinEstado_conservaElEstadoExistente() {
        Usuario existente = usuarioExistente(5L, 1, perfil(2L, "Editor"), "$2a$hashOriginal");
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(existente));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        Usuario resultado = usuarioService.guardarUsuario(request(5L, null, null));

        assertThat(resultado.getEstado()).isEqualTo(1);
        assertThat(resultado.getNombre()).isEqualTo("Nuevo Nombre");
    }

    /**
     * Regresión del bug real que existía antes de este fix:
     * {@code guardarUsuario(UsuarioRequest)} hacía {@code setClave(request.getClave())}
     * (null si no se manda clave nueva) directamente sobre la entidad gestionada
     * por JPA, y {@code guardarUsuario(Usuario)} intentaba "recuperar la clave
     * anterior" con un segundo {@code findById} dentro de la MISMA transacción —
     * que Hibernate resuelve devolviendo la misma instancia ya mutada (identity
     * map), así que terminaba copiando null sobre null. Ahora el hash real se
     * captura en una variable local (`claveActual`, un String inmutable) ANTES
     * de tocar la entidad, así que sobrevive a la mutación posterior.
     */
    @Test
    @DisplayName("Actualizar sin nueva clave: conserva el hash real existente (regresión del bug)")
    void actualizarSinNuevaClave_conservaElHashExistente() {
        Usuario existente = usuarioExistente(5L, 1, perfil(2L, "Editor"), "$2a$hashOriginal");
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(existente));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        Usuario resultado = usuarioService.guardarUsuario(request(5L, null, null));

        assertThat(resultado.getClave()).isEqualTo("$2a$hashOriginal");
    }

    @Test
    @DisplayName("Actualizar con nueva clave: la encripta y reemplaza el hash anterior")
    void actualizarConNuevaClave_laEncriptaYReemplazaElHashAnterior() {
        Usuario existente = usuarioExistente(5L, 1, perfil(2L, "Editor"), "$2a$hashOriginal");
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(existente));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        Usuario resultado = usuarioService.guardarUsuario(request(5L, "nuevaClave123", null));

        assertThat(resultado.getClave()).isNotEqualTo("nuevaClave123");
        assertThat(resultado.getClave()).isNotEqualTo("$2a$hashOriginal");
        assertThat(usuarioService.verificarContrasena("nuevaClave123", resultado.getClave())).isTrue();
    }

    @Test
    @DisplayName("Actualizar con estado explícito false: lo sobrescribe a inactivo (0)")
    void actualizarConEstadoExplicitoFalse_loSobrescribeAInactivo() {
        Usuario existente = usuarioExistente(5L, 1, perfil(2L, "Editor"), "$2a$hashOriginal");
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(existente));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        Usuario resultado = usuarioService.guardarUsuario(request(5L, null, false));

        assertThat(resultado.getEstado()).isEqualTo(0);
    }

    /**
     * Hallazgo real detectado escribiendo este test, no un comportamiento deseado:
     * para un usuario NUEVO, {@code guardarUsuario(Usuario)} fuerza
     * {@code estado = 1} incondicionalmente al final de la rama "nuevo usuario"
     * (después de encriptar la clave), pisando lo que
     * {@code guardarUsuario(UsuarioRequest)} ya había copiado desde
     * {@code request.getEstado()}. Si el formulario de creación alguna vez manda
     * {@code estado=false} (hoy el frontend no lo hace), el usuario se crearía
     * igual como activo. Este test documenta el comportamiento actual.
     */
    @Test
    @DisplayName("Crear usuario nuevo con estado=false en el request: igual queda activo (hallazgo)")
    void crearUsuarioNuevoConEstadoFalse_igualQuedaActivo() {
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        Usuario resultado = usuarioService.guardarUsuario(request(null, "secret123", false));

        assertThat(resultado.getEstado()).isEqualTo(1);
    }

    /**
     * HALLAZGO: la validación interna lanza {@code IllegalArgumentException},
     * pero el {@code catch (Exception e)} genérico de
     * {@code guardarUsuario(Usuario)} la vuelve a envolver en un
     * {@code RuntimeException} con un prefijo redundante ("Error al guardar el
     * usuario: "). {@code GlobalExceptionHandler} igual devuelve 400 para
     * cualquier {@code RuntimeException} de negocio, así que el HTTP status no
     * se rompe — pero se pierde el tipo específico de la excepción y el mensaje
     * queda duplicado/confuso para quien lo lea en un log.
     */
    @Test
    @DisplayName("Crear usuario nuevo sin clave: lanza excepción (envuelta) y no guarda")
    void crearUsuarioNuevoSinClave_lanzaExcepcion() {
        assertThatThrownBy(() -> usuarioService.guardarUsuario(request(null, null, null)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("contraseña es obligatoria");

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    @DisplayName("Crear usuario nuevo con clave: la encripta (no la guarda en texto plano)")
    void crearUsuarioNuevoConClave_laEncripta() {
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        Usuario resultado = usuarioService.guardarUsuario(request(null, "secret123", null));

        assertThat(resultado.getClave()).isNotEqualTo("secret123");
        assertThat(usuarioService.verificarContrasena("secret123", resultado.getClave())).isTrue();
    }

    @Test
    @DisplayName("Actualizar asignando perfilId: resuelve el Perfil desde el repositorio")
    void actualizarConPerfilId_resuelveElPerfil() {
        Usuario existente = usuarioExistente(5L, 1, perfil(2L, "Editor"), "$2a$hashOriginal");
        Perfil nuevoPerfil = perfil(3L, "Supervisor");
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(existente));
        when(perfilRepository.findById(3L)).thenReturn(Optional.of(nuevoPerfil));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioRequest req = request(5L, null, null);
        req.setPerfilId(3L);

        Usuario resultado = usuarioService.guardarUsuario(req);

        assertThat(resultado.getPerfil().getId()).isEqualTo(3L);
    }

    // ===================== validarCambioPerfilPropio =====================

    @Test
    @DisplayName("Admin intenta cambiar su propio perfil a otro: lanza AccesoDenegadoException")
    void validarCambioPerfilPropio_adminCambiaAOtroPerfil_lanzaExcepcion() {
        Perfil admin = perfil(1L, "Administrador");
        Perfil editor = perfil(2L, "Editor");

        assertThatThrownBy(() -> usuarioService.validarCambioPerfilPropio(admin, editor))
                .isInstanceOf(UsuarioService.AccesoDenegadoException.class)
                .hasMessageContaining("No puedes cambiar tu propio perfil");
    }

    @Test
    @DisplayName("Admin 'cambia' a su mismo perfil (mismo ID): no lanza excepción")
    void validarCambioPerfilPropio_adminMismoPerfil_noLanza() {
        Perfil admin = perfil(1L, "Administrador");

        usuarioService.validarCambioPerfilPropio(admin, perfil(1L, "Administrador"));
        // no exception
    }

    @Test
    @DisplayName("Usuario no administrador cambia su propio perfil: no lanza excepción")
    void validarCambioPerfilPropio_noAdministrador_noLanza() {
        Perfil editor = perfil(2L, "Editor");

        usuarioService.validarCambioPerfilPropio(editor, perfil(3L, "Supervisor"));
        // no exception
    }

    // ===================== validarDemocionUltimoAdmin =====================

    @Test
    @DisplayName("Degradar al único administrador activo: lanza AccesoDenegadoException")
    void validarDemocionUltimoAdmin_unicoAdminActivo_lanzaExcepcion() {
        Usuario existente = usuarioExistente(1L, 1, perfil(1L, "Administrador"), "hash");
        when(usuarioRepository.countByEstadoAndPerfil_NombreIgnoreCase(1, "Administrador")).thenReturn(1L);

        assertThatThrownBy(() -> usuarioService.validarDemocionUltimoAdmin(existente, perfil(2L, "Editor")))
                .isInstanceOf(UsuarioService.AccesoDenegadoException.class)
                .hasMessageContaining("único administrador");
    }

    @Test
    @DisplayName("Degradar a un administrador habiendo otros activos: no lanza excepción")
    void validarDemocionUltimoAdmin_hayOtrosAdmins_noLanza() {
        Usuario existente = usuarioExistente(1L, 1, perfil(1L, "Administrador"), "hash");
        when(usuarioRepository.countByEstadoAndPerfil_NombreIgnoreCase(1, "Administrador")).thenReturn(2L);

        usuarioService.validarDemocionUltimoAdmin(existente, perfil(2L, "Editor"));
        // no exception
    }

    @Test
    @DisplayName("Cambiar de perfil sin dejar de ser Administrador: no consulta el conteo")
    void validarDemocionUltimoAdmin_siSigueSiendoAdmin_noConsultaConteo() {
        Usuario existente = usuarioExistente(1L, 1, perfil(1L, "Administrador"), "hash");

        usuarioService.validarDemocionUltimoAdmin(existente, perfil(9L, "Administrador"));

        verify(usuarioRepository, never()).countByEstadoAndPerfil_NombreIgnoreCase(any(), any());
    }

    // ===================== validarPuedeEliminar =====================

    @Test
    @DisplayName("Eliminar la propia cuenta: lanza AccesoDenegadoException aunque no sea admin")
    void validarPuedeEliminar_propiaCuenta_lanzaExcepcion() {
        Usuario usuario = usuarioExistente(7L, 1, perfil(2L, "Editor"), "hash");

        assertThatThrownBy(() -> usuarioService.validarPuedeEliminar(usuario, 7L))
                .isInstanceOf(UsuarioService.AccesoDenegadoException.class)
                .hasMessageContaining("No puedes eliminar tu propia cuenta");
    }

    @Test
    @DisplayName("Eliminar al único administrador activo (otro usuario): lanza AccesoDenegadoException")
    void validarPuedeEliminar_unicoAdminActivo_lanzaExcepcion() {
        Usuario admin = usuarioExistente(1L, 1, perfil(1L, "Administrador"), "hash");
        when(usuarioRepository.countByEstadoAndPerfil_NombreIgnoreCase(1, "Administrador")).thenReturn(1L);

        assertThatThrownBy(() -> usuarioService.validarPuedeEliminar(admin, 99L))
                .isInstanceOf(UsuarioService.AccesoDenegadoException.class)
                .hasMessageContaining("único administrador activo");
    }

    @Test
    @DisplayName("Eliminar a un administrador habiendo otros activos: no lanza excepción")
    void validarPuedeEliminar_adminPeroHayOtros_noLanza() {
        Usuario admin = usuarioExistente(1L, 1, perfil(1L, "Administrador"), "hash");
        when(usuarioRepository.countByEstadoAndPerfil_NombreIgnoreCase(1, "Administrador")).thenReturn(2L);

        usuarioService.validarPuedeEliminar(admin, 99L);
        // no exception
    }

    @Test
    @DisplayName("Eliminar a un usuario no administrador: no lanza excepción ni consulta el conteo")
    void validarPuedeEliminar_noEsAdmin_noLanza() {
        Usuario editor = usuarioExistente(2L, 1, perfil(2L, "Editor"), "hash");

        usuarioService.validarPuedeEliminar(editor, 99L);

        verify(usuarioRepository, never()).countByEstadoAndPerfil_NombreIgnoreCase(any(), any());
    }

    // ===================== validarPuedeDesactivar =====================

    @Test
    @DisplayName("Desactivar la propia cuenta: lanza AccesoDenegadoException")
    void validarPuedeDesactivar_propiaCuenta_lanzaExcepcion() {
        Usuario usuario = usuarioExistente(7L, 1, perfil(2L, "Editor"), "hash");

        assertThatThrownBy(() -> usuarioService.validarPuedeDesactivar(usuario, 7L))
                .isInstanceOf(UsuarioService.AccesoDenegadoException.class)
                .hasMessageContaining("No puedes desactivar tu propia cuenta");
    }

    @Test
    @DisplayName("Desactivar al único administrador activo: lanza AccesoDenegadoException")
    void validarPuedeDesactivar_unicoAdminActivo_lanzaExcepcion() {
        Usuario admin = usuarioExistente(1L, 1, perfil(1L, "Administrador"), "hash");
        when(usuarioRepository.countByEstadoAndPerfil_NombreIgnoreCase(1, "Administrador")).thenReturn(1L);

        assertThatThrownBy(() -> usuarioService.validarPuedeDesactivar(admin, 99L))
                .isInstanceOf(UsuarioService.AccesoDenegadoException.class)
                .hasMessageContaining("único administrador activo");
    }

    @Test
    @DisplayName("Desactivar a un admin ya inactivo: no lanza excepción ni consulta el conteo")
    void validarPuedeDesactivar_yaInactivo_noLanza() {
        Usuario adminInactivo = usuarioExistente(1L, 0, perfil(1L, "Administrador"), "hash");

        usuarioService.validarPuedeDesactivar(adminInactivo, 99L);

        verify(usuarioRepository, never()).countByEstadoAndPerfil_NombreIgnoreCase(any(), any());
    }

    // ===================== verificarContrasena =====================

    @Test
    @DisplayName("Verificar contraseña: coincide con su hash BCrypt")
    void verificarContrasena_claveCorrecta_devuelveTrue() {
        String hash = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("secret123");

        assertThat(usuarioService.verificarContrasena("secret123", hash)).isTrue();
    }

    @Test
    @DisplayName("Verificar contraseña: no coincide con un hash de otra clave")
    void verificarContrasena_claveIncorrecta_devuelveFalse() {
        String hash = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("secret123");

        assertThat(usuarioService.verificarContrasena("otraClave", hash)).isFalse();
    }

    // ===================== cambiarEstadoUsuario =====================

    @Test
    @DisplayName("Cambiar estado de un usuario activo: pasa a inactivo (1 -> 0)")
    void cambiarEstadoUsuarioActivo_loDesactiva() {
        Usuario activo = usuarioExistente(3L, 1, perfil(2L, "Editor"), "hash");
        when(usuarioRepository.findById(3L)).thenReturn(Optional.of(activo));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        Optional<Usuario> resultado = usuarioService.cambiarEstadoUsuario(3L);

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getEstado()).isEqualTo(0);
    }

    @Test
    @DisplayName("Cambiar estado de un usuario inactivo: pasa a activo (0 -> 1)")
    void cambiarEstadoUsuarioInactivo_loActiva() {
        Usuario inactivo = usuarioExistente(4L, 0, perfil(2L, "Editor"), "hash");
        when(usuarioRepository.findById(4L)).thenReturn(Optional.of(inactivo));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        Optional<Usuario> resultado = usuarioService.cambiarEstadoUsuario(4L);

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getEstado()).isEqualTo(1);
    }

    @Test
    @DisplayName("Cambiar estado de un usuario eliminado (2): no lo modifica, igual lo guarda")
    void cambiarEstadoUsuarioEliminado_noLoModifica() {
        Usuario eliminado = usuarioExistente(6L, 2, perfil(2L, "Editor"), "hash");
        when(usuarioRepository.findById(6L)).thenReturn(Optional.of(eliminado));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        Optional<Usuario> resultado = usuarioService.cambiarEstadoUsuario(6L);

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getEstado()).isEqualTo(2);
    }

    @Test
    @DisplayName("Eliminar usuario: borrado lógico con estado 2")
    void eliminarUsuario_realizaBorradoLogicoConEstado2() {
        Usuario existente = usuarioExistente(8L, 1, perfil(2L, "Editor"), "hash");
        when(usuarioRepository.findById(8L)).thenReturn(Optional.of(existente));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        usuarioService.eliminarUsuario(8L);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertThat(captor.getValue().getEstado()).isEqualTo(2);
    }
}
