package com.sistemadequejas.repository;

import com.sistemadequejas.model.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificacionRepository extends JpaRepository<Notificacion, Integer> {

    List<Notificacion> findTop200ByOrderByFechaEnvioDesc();

    List<Notificacion> findByEstadoEnvioAndReintentosLessThan(String estadoEnvio, Integer maxReintentos);
}
