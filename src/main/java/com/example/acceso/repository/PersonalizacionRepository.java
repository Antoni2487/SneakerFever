package com.example.acceso.repository;

import com.example.acceso.model.Personalizacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PersonalizacionRepository extends JpaRepository<Personalizacion, Long> {

    // Consultas para Logo
    Optional<Personalizacion> findByTipo(String tipo);

    @Query("SELECT p FROM Personalizacion p WHERE p.tipo = 'LOGO'")
    Optional<Personalizacion> findLogo();

    // Consultas para Slides con JOIN FETCH para evitar N+1
    @Query("SELECT p FROM Personalizacion p LEFT JOIN FETCH p.marca WHERE p.tipo = 'SLIDE' ORDER BY p.orden ASC")
    List<Personalizacion> findAllSlidesWithMarca();

    @Query("SELECT p FROM Personalizacion p WHERE p.tipo = 'SLIDE' ORDER BY p.orden ASC")
    List<Personalizacion> findAllSlides();

    // Buscar slide por orden específico
    Optional<Personalizacion> findByTipoAndOrden(String tipo, Integer orden);

    // Buscar slides de una marca específica
    @Query("SELECT p FROM Personalizacion p LEFT JOIN FETCH p.marca m WHERE p.tipo = 'SLIDE' AND m.id = :marcaId ORDER BY p.orden ASC")
    List<Personalizacion> findSlidesByMarcaId(@Param("marcaId") Long marcaId);

    // Contar slides existentes
    long countByTipo(String tipo);

    // Verificar si existe un slide en un orden específico
    boolean existsByTipoAndOrden(String tipo, Integer orden);

    // Obtener el máximo orden de slides
    @Query("SELECT COALESCE(MAX(p.orden), 0) FROM Personalizacion p WHERE p.tipo = 'SLIDE'")
    Integer findMaxOrdenSlide();

    // Eliminar por tipo y orden
    void deleteByTipoAndOrden(String tipo, Integer orden);

    // Buscar slides sin marca asignada
    @Query("SELECT p FROM Personalizacion p WHERE p.tipo = 'SLIDE' AND p.marca IS NULL ORDER BY p.orden ASC")
    List<Personalizacion> findSlidesWithoutMarca();
}