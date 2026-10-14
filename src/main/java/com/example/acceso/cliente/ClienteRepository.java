package com.example.acceso.cliente;

import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import com.example.acceso.cliente.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    Optional<Cliente> findByDocumento(String documento);

    boolean existsByDocumentoAndEstadoNot(String documento, Integer estado);
    boolean existsByDocumentoAndIdNotAndEstadoNot(String documento, Long id, Integer estado);
    List<Cliente> findAllByEstado(Integer estado);
    List<Cliente> findByNombreContainingIgnoreCaseAndEstado(String nombre, Integer estado);
    long countByEstado(Integer estado);
    Optional<Cliente> findByCorreo(String correo);

    /** Listado paginado del panel (excluye eliminados); busca por nombre, documento o teléfono. */
    @Query(value = "SELECT c FROM Cliente c WHERE c.estado <> 2 AND ("
            + "LOWER(c.nombre) LIKE LOWER(CONCAT('%', :q, '%')) "
            + "OR c.documento LIKE CONCAT('%', :q, '%') OR c.telefono LIKE CONCAT('%', :q, '%'))",
            countQuery = "SELECT COUNT(c) FROM Cliente c WHERE c.estado <> 2 AND ("
            + "LOWER(c.nombre) LIKE LOWER(CONCAT('%', :q, '%')) "
            + "OR c.documento LIKE CONCAT('%', :q, '%') OR c.telefono LIKE CONCAT('%', :q, '%'))")
    Page<Cliente> buscarPagina(@Param("q") String q, Pageable pageable);
}
