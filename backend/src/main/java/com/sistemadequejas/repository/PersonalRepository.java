package com.sistemadequejas.repository;

import com.sistemadequejas.model.Personal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PersonalRepository extends JpaRepository<Personal, Integer> {

    Optional<Personal> findByCorreo(String correo);

    boolean existsByCorreo(String correo);

    List<Personal> findByActivoTrueOrderByNombreCompleto();

    long countByRolNombre(String nombreRol);
}
