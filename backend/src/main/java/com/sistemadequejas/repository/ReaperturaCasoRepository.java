package com.sistemadequejas.repository;

import com.sistemadequejas.model.Caso;
import com.sistemadequejas.model.ReaperturaCaso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReaperturaCasoRepository extends JpaRepository<ReaperturaCaso, Integer> {

    List<ReaperturaCaso> findByCasoOrderByFechaSolicitudDesc(Caso caso);
}
