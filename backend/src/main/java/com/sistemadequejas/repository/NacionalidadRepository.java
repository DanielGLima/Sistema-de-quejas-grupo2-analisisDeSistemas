package com.sistemadequejas.repository;

import com.sistemadequejas.model.Nacionalidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NacionalidadRepository extends JpaRepository<Nacionalidad, Integer> {

    boolean existsByNombreAndActivoTrue(String nombre);

    List<Nacionalidad> findByActivoTrueOrderByIdNacionalidad();
}
