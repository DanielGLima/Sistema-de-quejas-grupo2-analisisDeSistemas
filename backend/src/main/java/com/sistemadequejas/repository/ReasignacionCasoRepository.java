package com.sistemadequejas.repository;

import com.sistemadequejas.model.Caso;
import com.sistemadequejas.model.ReasignacionCaso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReasignacionCasoRepository extends JpaRepository<ReasignacionCaso, Integer> {

    List<ReasignacionCaso> findByCasoOrderByFechaReasignacionDesc(Caso caso);
}
