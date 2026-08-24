package com.sistemadequejas.repository;

import com.sistemadequejas.model.Caso;
import com.sistemadequejas.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CasoRepository extends JpaRepository<Caso, Integer> {

    List<Caso> findByUsuarioOrderByFechaCreacionDesc(Usuario usuario);
}
