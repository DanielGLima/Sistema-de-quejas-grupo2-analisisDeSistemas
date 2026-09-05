package com.sistemadequejas.service;

import com.sistemadequejas.model.Caso;
import com.sistemadequejas.model.EstadoCaso;
import com.sistemadequejas.model.HistorialEstadoCaso;
import com.sistemadequejas.repository.HistorialEstadoCasoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HistorialEstadoCasoService {

    @Autowired
    private HistorialEstadoCasoRepository historialEstadoCasoRepository;

    public HistorialEstadoCaso registrarCambio(Caso caso, EstadoCaso estadoAnterior, EstadoCaso estadoNuevo, String observacion) {
        HistorialEstadoCaso historial = new HistorialEstadoCaso();
        historial.setCaso(caso);
        historial.setEstadoAnterior(estadoAnterior);
        historial.setEstadoNuevo(estadoNuevo);
        historial.setObservacion(observacion);
        return historialEstadoCasoRepository.save(historial);
    }

    public List<HistorialEstadoCaso> findByCaso(Caso caso) {
        return historialEstadoCasoRepository.findByCasoOrderByFechaCambioDesc(caso);
    }
}
