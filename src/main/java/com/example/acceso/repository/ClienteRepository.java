package com.example.acceso.repository;

import com.example.acceso.model.Cliente;
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

}
