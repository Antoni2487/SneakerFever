package com.example.acceso.marca;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MarcaRepository extends JpaRepository<Marca, Long> {

    Optional<Marca> findByNombre(String nombre);
    @EntityGraph(attributePaths = {"imagenes"})
    List<Marca> findAllByEstado(Integer estado);
    @EntityGraph(attributePaths = {"imagenes"})
    List<Marca> findAllByEstadoOrderByNombreAsc(Integer estado);
    long countByEstado(Integer estado);
    @EntityGraph(attributePaths = {"imagenes"})
    List<Marca> findByNombreContainingIgnoreCaseAndEstado(String nombre, Integer estado);
    @EntityGraph(attributePaths = {"imagenes"})
    List<Marca> findByNombreContainingIgnoreCase(String nombre);
    @EntityGraph(attributePaths = {"imagenes"})
    List<Marca> findByImagenIsNotNullAndEstado(Integer estado);
    @EntityGraph(attributePaths = {"imagenes"})
    List<Marca> findByImagenIsNullAndEstado(Integer estado);
    @Query("SELECT DISTINCT b FROM Marca b LEFT JOIN FETCH b.imagenes")
    List<Marca> findAllWithImages();

    boolean existsByNombreAndEstadoNot(String nombre, Integer estado);
    boolean existsByNombreAndIdNotAndEstadoNot(String nombre, Long id, Integer estado);

}
