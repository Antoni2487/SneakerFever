package com.example.acceso.brand;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BrandRepository extends JpaRepository<Brand, Long> {

    Optional<Brand> findByNombre(String nombre);
    @EntityGraph(attributePaths = {"imagenes"})
    List<Brand> findAllByEstado(Integer estado);
    @EntityGraph(attributePaths = {"imagenes"})
    List<Brand> findAllByEstadoOrderByNombreAsc(Integer estado);
    long countByEstado(Integer estado);
    @EntityGraph(attributePaths = {"imagenes"})
    List<Brand> findByNombreContainingIgnoreCaseAndEstado(String nombre, Integer estado);
    @EntityGraph(attributePaths = {"imagenes"})
    List<Brand> findByNombreContainingIgnoreCase(String nombre);
    @EntityGraph(attributePaths = {"imagenes"})
    List<Brand> findByImagenIsNotNullAndEstado(Integer estado);
    @EntityGraph(attributePaths = {"imagenes"})
    List<Brand> findByImagenIsNullAndEstado(Integer estado);
    @Query("SELECT DISTINCT b FROM Brand b LEFT JOIN FETCH b.imagenes")
    List<Brand> findAllWithImages();

    boolean existsByNombreAndEstadoNot(String nombre, Integer estado);
    boolean existsByNombreAndIdNotAndEstadoNot(String nombre, Long id, Integer estado);

}
