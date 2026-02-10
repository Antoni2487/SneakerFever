package com.example.acceso.repository;


import com.example.acceso.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByNombre(String nombre);
    boolean existsByNombre(String nombre);
    boolean existsByNombreAndIdNot(String nombre, Long id);

    List<Category> findAllByEstado(Integer estado);
    List<Category> findAllByEstadoOrderByNombreAsc(Integer estado);
    List<Category> findAllByEstadoNot(Integer estado);
    long countByEstado(Integer estado);
    long countByEstadoNot(Integer estado);

    List<Category> findByNombreContainingIgnoreCaseAndEstado(String nombre, Integer estado);
    List<Category> findByNombreContainingIgnoreCase(String nombre);
}

