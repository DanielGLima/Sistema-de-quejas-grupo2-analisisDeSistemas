package com.sistemadequejas.repository;

import com.sistemadequejas.model.Caso;
import com.sistemadequejas.model.RespuestaCaso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RespuestaCasoRepository extends JpaRepository<RespuestaCaso, Integer> {

    List<RespuestaCaso> findByCasoOrderByFechaRespuestaDesc(Caso caso);
}
