package com.example.acceso.usuario;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Métodos para encontrar usuarios por sus propiedades únicas
    Optional<Usuario> findByUsuario(String usuario);

    /**
     * Variante de {@link #findByUsuario(String)} para el flujo de autenticación:
     * trae el Perfil y sus Opciones en la misma query.
     *
     * <p>Hace falta porque {@code UsuarioResponse.from()} serializa el perfil
     * completo (nombre, descripción y la lista de opciones) y, en el flujo del
     * token, el Usuario se carga en {@code JwtAuthenticationFilter} — un filtro
     * de servlet que corre ANTES de que el interceptor de open-in-view abra el
     * EntityManager de la petición. La transacción del servicio se cierra al
     * salir del filtro, así que para cuando el controlador serializa, el proxy
     * del Perfil ya no tiene sesión: LazyInitializationException.
     *
     * <p>Se usa {@code @EntityGraph} y no {@code JOIN FETCH} por dos razones:
     * genera LEFT OUTER JOIN, de modo que un usuario sin perfil
     * ({@code id_perfil} es nullable) sigue apareciendo en el resultado en vez
     * de desaparecer; y permite declarar los dos niveles ({@code perfil} y
     * {@code perfil.opciones}) sin escribir la consulta a mano. Es además el
     * mismo patrón que ya usa {@link #findAllByEstadoNot(Integer)}.
     *
     * <p>La relación Usuario→Perfil se deja LAZY en la entidad a propósito: el
     * fetch se pide solo en las consultas que realmente serializan el perfil.
     */
    @EntityGraph(attributePaths = {"perfil", "perfil.opciones"})
    Optional<Usuario> findWithPerfilByUsuario(String usuario);

    Optional<Usuario> findByCorreo(String correo);

    // Métodos para verificar la existencia de usuarios
    boolean existsByUsuario(String usuario);

    boolean existsByCorreo(String correo);

    // Métodos para el borrado lógico
    @EntityGraph(attributePaths = {"perfil", "perfil.opciones"})
    List<Usuario> findAllByEstadoNot(Integer estado);

    long countByEstadoNot(Integer estado);

    long countByEstadoAndPerfil_NombreIgnoreCase(Integer estado, String nombrePerfil);
}