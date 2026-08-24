package com.sistemadequejas.service;

import com.sistemadequejas.model.EstadoCaso;
import com.sistemadequejas.repository.EstadoCasoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class EstadoCasoService {

    @Autowired
    private EstadoCasoRepository estadoCasoRepository;

    public List<EstadoCaso> findAll() {
        return estadoCasoRepository.findAll();
    }

    public Optional<EstadoCaso> findById(Integer id) {
        return estadoCasoRepository.findById(id);
    }

    public Optional<EstadoCaso> findByNombre(String nombre) {
        return estadoCasoRepository.findByNombre(nombre);
    }

    public EstadoCaso save(EstadoCaso estadoCaso) {
        return estadoCasoRepository.save(estadoCaso);
    }

    public void deleteById(Integer id) {
        estadoCasoRepository.deleteById(id);
    }
}
