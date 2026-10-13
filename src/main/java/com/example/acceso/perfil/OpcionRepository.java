package com.example.acceso.perfil;

import com.example.acceso.perfil.Opcion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OpcionRepository extends JpaRepository<Opcion, Long> {

    List<Opcion> findByPermisoIsNull();
}