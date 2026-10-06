package com.sistemadequejas.repository;

import com.sistemadequejas.model.Caso;
import com.sistemadequejas.model.Personal;
import com.sistemadequejas.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface CasoRepository extends JpaRepository<Caso, Integer>, JpaSpecificationExecutor<Caso> {

    List<Caso> findByUsuarioOrderByFechaCreacionDesc(Usuario usuario);

    long countByPersonalAsignadoAndEstadoCasoNombreNotIn(Personal personal, Collection<String> estadosFinales);

    long countBySucursalIdSucursalAndEstadoCasoNombreNotIn(Integer idSucursal, Collection<String> estadosFinales);

    long countByTipoCasoIdTipoCasoAndEstadoCasoNombreNotIn(Integer idTipoCaso, Collection<String> estadosFinales);

    long countByCategoriaCasoIdCategoriaAndEstadoCasoNombreNotIn(Integer idCategoria, Collection<String> estadosFinales);

    long countByEstadoCasoIdEstadoAndEstadoCasoNombreNotIn(Integer idEstado, Collection<String> estadosFinales);
}
