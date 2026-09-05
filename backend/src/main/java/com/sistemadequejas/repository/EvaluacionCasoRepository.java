package com.sistemadequejas.repository;

import com.sistemadequejas.model.EvaluacionCaso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EvaluacionCasoRepository extends JpaRepository<EvaluacionCaso, Integer> {
}
