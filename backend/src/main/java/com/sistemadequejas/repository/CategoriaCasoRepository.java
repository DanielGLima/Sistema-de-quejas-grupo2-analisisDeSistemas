package com.sistemadequejas.repository;

import com.sistemadequejas.model.CategoriaCaso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoriaCasoRepository extends JpaRepository<CategoriaCaso, Integer> {
}
