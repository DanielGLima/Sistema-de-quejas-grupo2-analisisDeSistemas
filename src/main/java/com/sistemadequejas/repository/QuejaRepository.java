package com.sistemadequejas.repository;

import com.sistemadequejas.model.Queja;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuejaRepository extends JpaRepository<Queja, Integer> {
}
