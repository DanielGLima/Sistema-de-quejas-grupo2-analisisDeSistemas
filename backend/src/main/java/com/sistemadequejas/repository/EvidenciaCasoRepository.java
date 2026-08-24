package com.sistemadequejas.repository;

import com.sistemadequejas.model.EvidenciaCaso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EvidenciaCasoRepository extends JpaRepository<EvidenciaCaso, Integer> {
}
