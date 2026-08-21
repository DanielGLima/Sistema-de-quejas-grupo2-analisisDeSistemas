package com.sistemadequejas.service;

import com.sistemadequejas.model.HistorialEstado;
import com.sistemadequejas.repository.HistorialEstadoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class HistorialEstadoService {

    @Autowired
    private HistorialEstadoRepository historialEstadoRepository;

    public List<HistorialEstado> findAll() {
        return historialEstadoRepository.findAll();
    }

    public Optional<HistorialEstado> findById(Integer id) {
        return historialEstadoRepository.findById(id);
    }

    public HistorialEstado save(HistorialEstado historialEstado) {
        return historialEstadoRepository.save(historialEstado);
    }

    public void deleteById(Integer id) {
        historialEstadoRepository.deleteById(id);
    }
}
