package com.sistemadequejas.repository;

import com.sistemadequejas.model.TipoQueja;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TipoQuejaRepository extends JpaRepository<TipoQueja, Integer> {
}
