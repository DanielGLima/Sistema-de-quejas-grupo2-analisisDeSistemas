package com.sistemadequejas.repository;

import com.sistemadequejas.model.BitacoraAuditoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BitacoraAuditoriaRepository extends JpaRepository<BitacoraAuditoria, Integer> {
}
