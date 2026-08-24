package com.sistemadequejas.repository;

import com.sistemadequejas.model.EstadoCaso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EstadoCasoRepository extends JpaRepository<EstadoCaso, Integer> {

    Optional<EstadoCaso> findByNombre(String nombre);
}
