package com.sistemadequejas.repository;

import com.sistemadequejas.model.RecuperacionContrasena;
import com.sistemadequejas.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RecuperacionContrasenaRepository extends JpaRepository<RecuperacionContrasena, Integer> {

    Optional<RecuperacionContrasena> findFirstByUsuarioAndCodigoTokenAndUsadoFalseOrderByFechaCreacionDesc(
            Usuario usuario, String codigoToken);
}
