package com.example.acceso.usuario;

import com.example.acceso.usuario.Usuario;
import com.example.acceso.usuario.UsuarioRepository;
import com.example.acceso.perfil.Perfil;
import com.example.acceso.perfil.PerfilRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UsuarioService {

    private static final String PERFIL_ADMINISTRADOR = "Administrador";

    private final UsuarioRepository usuarioRepository;
    private final PerfilRepository perfilRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PerfilRepository perfilRepository) {
        this.usuarioRepository = usuarioRepository;
        this.perfilRepository = perfilRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @Transactional(readOnly = true)
    public List<Usuario> listarUsuarios() {

        return usuarioRepository.findAllByEstadoNot(2);
    }

    @Transactional
    public Usuario guardarUsuario(Usuario usuario) {
        try {
            // Validaciones adicionales
            if (usuario.getNombre() == null || usuario.getNombre().trim().isEmpty()) {
                throw new IllegalArgumentException("El nombre es obligatorio");
            }

            if (usuario.getUsuario() == null || usuario.getUsuario().trim().isEmpty()) {
                throw new IllegalArgumentException("El usuario es obligatorio");
            }

            if (usuario.getCorreo() == null || usuario.getCorreo().trim().isEmpty()) {
                throw new IllegalArgumentException("El correo es obligatorio");
            }

            // Normalizar datos
            usuario.setNombre(usuario.getNombre().trim());
            usuario.setUsuario(usuario.getUsuario().trim().toLowerCase());
            usuario.setCorreo(usuario.getCorreo().trim().toLowerCase());

            // Manejo de contraseñas
            if (usuario.getId() != null) {
                // Usuario existente - actualización. La decisión de mantener
                // el hash actual o cifrar una clave nueva se resuelve en
                // guardarUsuario(UsuarioRequest) — el único punto que sabe si
                // el valor viene en texto plano desde el formulario o ya es
                // el hash real leído de BD. No se repite acá con un
                // findById: dentro de la misma transacción (self-invocation
                // desde el otro overload), Hibernate devolvería la MISMA
                // instancia ya mutada, no una copia fresca para comparar —
                // así se rompía antes la contraseña en cada edición que no
                // mandaba una nueva (ver UsuarioServiceTest).
                if (usuario.getClave() == null || usuario.getClave().trim().isEmpty()) {
                    throw new IllegalArgumentException("La contraseña no puede quedar vacía");
                }
            } else {
                // Nuevo usuario
                if (usuario.getClave() == null || usuario.getClave().trim().isEmpty()) {
                    throw new IllegalArgumentException("La contraseña es obligatoria para nuevos usuarios");
                }

                usuario.setClave(passwordEncoder.encode(usuario.getClave().trim()));

                usuario.setEstado(1);
            }

            return usuarioRepository.save(usuario);

        } catch (DataIntegrityViolationException e) {

            String message = e.getMessage().toLowerCase();
            if (message.contains("usuario")) {
                throw new IllegalArgumentException("El nombre de usuario ya existe");
            } else if (message.contains("correo") || message.contains("email")) {
                throw new IllegalArgumentException("El correo electrónico ya está registrado");
            } else {
                throw new IllegalArgumentException("Error de integridad de datos");
            }
        } catch (Exception e) {
            throw new RuntimeException("Error al guardar el usuario: " + e.getMessage(), e);
        }
    }

    @Transactional
    public Usuario guardarUsuario(UsuarioRequest request) {
        boolean esNuevo = request.getId() == null;
        Usuario usuario;
        String claveActual = null;

        if (esNuevo) {
            usuario = new Usuario();
        } else {
            usuario = obtenerUsuarioPorId(request.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado para actualizar"));
            // Capturado ANTES de mutar `usuario`: es un String (inmutable),
            // no la referencia a la entidad gestionada por JPA, así que no le
            // afecta lo que se le haga a `usuario` después (ver el porqué en
            // guardarUsuario(Usuario)).
            claveActual = usuario.getClave();
        }

        usuario.setNombre(request.getNombre());
        usuario.setUsuario(request.getUsuario());
        usuario.setCorreo(request.getCorreo());

        boolean hayClaveNueva = request.getClave() != null && !request.getClave().isBlank();
        if (esNuevo) {
            // Usuario nuevo: guardarUsuario(Usuario) exige y cifra la clave
            // él mismo (esa rama no tiene el problema de instancia
            // compartida, siempre es una entidad nueva).
            usuario.setClave(request.getClave());
        } else if (hayClaveNueva) {
            // Clave nueva en texto plano: se cifra acá mismo, es el único
            // lugar que sabe que este valor viene del formulario y no ya
            // está hasheado en BD.
            usuario.setClave(passwordEncoder.encode(request.getClave().trim()));
        } else {
            // Sin clave nueva: se conserva el hash real ya capturado.
            usuario.setClave(claveActual);
        }

        if (request.getEstado() != null) {
            usuario.setEstado(request.getEstado() ? 1 : 0);
        }

        if (request.getPerfilId() != null) {
            Perfil perfil = perfilRepository.findById(request.getPerfilId())
                    .orElseThrow(() -> new IllegalArgumentException("Perfil no encontrado"));
            usuario.setPerfil(perfil);
        }

        return guardarUsuario(usuario);
    }

    @Transactional(readOnly = true)
    public long contarUsuarios() {

        return usuarioRepository.countByEstadoNot(2);
    }

    @Transactional(readOnly = true)
    public Optional<Usuario> obtenerUsuarioPorId(Long id) {
        if (id == null || id <= 0) {
            return Optional.empty();
        }
        return usuarioRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Usuario> findByUsuario(String usuario) {
        return usuarioRepository.findByUsuario(usuario.trim().toLowerCase());
    }

    /**
     * Igual que {@link #findByUsuario(String)} pero con el Perfil y sus Opciones
     * ya inicializados. Es la que deben usar los puntos que serializan el perfil
     * o lo recorren fuera de la transacción (autenticación por token, login y
     * armado del menú); ver el javadoc de
     * {@code UsuarioRepository#findWithPerfilByUsuario}.
     */
    @Transactional(readOnly = true)
    public Optional<Usuario> findByUsuarioConPerfil(String usuario) {
        return usuarioRepository.findWithPerfilByUsuario(usuario.trim().toLowerCase());
    }

    @Transactional
    public void eliminarUsuario(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("ID de usuario invÃ¡lido");
        }


        Usuario usuario = obtenerUsuarioPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        usuario.setEstado(2); // 2 significa "eliminado"
        usuarioRepository.save(usuario);
    }

    @Transactional
    public Optional<Usuario> cambiarEstadoUsuario(Long id) {
        if (id == null || id <= 0) {
            return Optional.empty();
        }

        return obtenerUsuarioPorId(id).map(usuario -> {
            // Solo alterna entre 0 (inactivo) y 1 (activo)
            if (usuario.getEstado() == 1) {
                usuario.setEstado(0); // Desactivar
            } else if (usuario.getEstado() == 0) {
                usuario.setEstado(1); // Activar
            }
            // No se hace nada si el estado es 2 (eliminado)
            return usuarioRepository.save(usuario);
        });
    }


    @Transactional(readOnly = true)
    public boolean existeUsuario(String nombreUsuario) {
        if (nombreUsuario == null || nombreUsuario.trim().isEmpty()) {
            return false;
        }
        // Utiliza el método eficiente del repositorio
        return usuarioRepository.existsByUsuario(nombreUsuario.trim().toLowerCase());
    }


    @Transactional(readOnly = true)
    public boolean existeCorreo(String correo) {
        if (correo == null || correo.trim().isEmpty()) {
            return false;
        }
        // Utiliza el método eficiente del repositorio
        return usuarioRepository.existsByCorreo(correo.trim().toLowerCase());
    }

    public boolean verificarContrasena(String contrasenaTextoPlano, String contrasenaEncriptada) {
        return passwordEncoder.matches(contrasenaTextoPlano, contrasenaEncriptada);
    }

    // ===================== Reglas de "último administrador" =====================

    private boolean esAdministrador(Perfil perfil) {
        return perfil != null && PERFIL_ADMINISTRADOR.equalsIgnoreCase(perfil.getNombre());
    }

    @Transactional(readOnly = true)
    public long contarAdministradoresActivos() {
        return usuarioRepository.countByEstadoAndPerfil_NombreIgnoreCase(1, PERFIL_ADMINISTRADOR);
    }

    /**
     * No permite que un usuario cambie su propio perfil si eso implica dejar de ser Administrador.
     */
    public void validarCambioPerfilPropio(Perfil perfilActual, Perfil perfilNuevo) {
        if (esAdministrador(perfilActual) && perfilNuevo != null
                && !perfilActual.getId().equals(perfilNuevo.getId())) {
            throw new AccesoDenegadoException(
                    "⛔ ACCIÓN DENEGADA: No puedes cambiar tu propio perfil de Administrador.");
        }
    }

    /**
     * No permite bajar de Administrador a otro perfil al único administrador activo del sistema.
     */
    public void validarDemocionUltimoAdmin(Usuario usuarioExistente, Perfil perfilNuevo) {
        boolean eraAdmin = esAdministrador(usuarioExistente.getPerfil());
        boolean seraAdmin = esAdministrador(perfilNuevo);

        if (eraAdmin && !seraAdmin && contarAdministradoresActivos() <= 1) {
            throw new AccesoDenegadoException(
                    "⛔ No puedes cambiar el perfil del único administrador del sistema.");
        }
    }

    /**
     * No permite eliminar la propia cuenta ni al único administrador activo del sistema.
     */
    public void validarPuedeEliminar(Usuario usuario, Long usuarioLogueadoId) {
        if (usuarioLogueadoId != null && usuario.getId().equals(usuarioLogueadoId)) {
            throw new AccesoDenegadoException(
                    "⛔ ACCIÓN DENEGADA: No puedes eliminar tu propia cuenta.");
        }

        if (esAdministrador(usuario.getPerfil()) && usuario.getEstado() == 1
                && contarAdministradoresActivos() <= 1) {
            throw new AccesoDenegadoException(
                    "⛔ ACCIÓN DENEGADA: No puedes eliminar al único administrador activo del sistema.");
        }
    }

    /**
     * No permite desactivar la propia cuenta ni al único administrador activo del sistema.
     */
    public void validarPuedeDesactivar(Usuario usuario, Long usuarioLogueadoId) {
        if (usuarioLogueadoId != null && usuario.getId().equals(usuarioLogueadoId)) {
            throw new AccesoDenegadoException(
                    "⛔ ACCIÓN DENEGADA: No puedes desactivar tu propia cuenta.");
        }

        if (usuario.getEstado() == 1 && esAdministrador(usuario.getPerfil())
                && contarAdministradoresActivos() <= 1) {
            throw new AccesoDenegadoException(
                    "⛔ ACCIÓN DENEGADA: No puedes desactivar al único administrador activo del sistema.");
        }
    }

    public static class AccesoDenegadoException extends RuntimeException {
        public AccesoDenegadoException(String message) {
            super(message);
        }
    }
}