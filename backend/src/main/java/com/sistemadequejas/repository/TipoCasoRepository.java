package com.sistemadequejas.repository;

import com.sistemadequejas.model.TipoCaso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TipoCasoRepository extends JpaRepository<TipoCaso, Integer> {
}
