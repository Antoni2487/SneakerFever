package com.example.acceso.categoria;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    Optional<Categoria> findByNombre(String nombre);
    boolean existsByNombre(String nombre);
    boolean existsByNombreAndIdNot(String nombre, Long id);

    List<Categoria> findAllByEstado(Integer estado);
    List<Categoria> findAllByEstadoOrderByNombreAsc(Integer estado);
    List<Categoria> findAllByEstadoNot(Integer estado);
    long countByEstado(Integer estado);
    long countByEstadoNot(Integer estado);

    List<Categoria> findByNombreContainingIgnoreCaseAndEstado(String nombre, Integer estado);
    List<Categoria> findByNombreContainingIgnoreCase(String nombre);
}

