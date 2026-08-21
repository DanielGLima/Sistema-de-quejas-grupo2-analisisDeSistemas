package com.sistemadequejas.service;

import com.sistemadequejas.model.Estado;
import com.sistemadequejas.repository.EstadoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class EstadoService {

    @Autowired
    private EstadoRepository estadoRepository;

    public List<Estado> findAll() {
        return estadoRepository.findAll();
    }

    public Optional<Estado> findById(Integer id) {
        return estadoRepository.findById(id);
    }

    public Estado save(Estado estado) {
        return estadoRepository.save(estado);
    }

    public void deleteById(Integer id) {
        estadoRepository.deleteById(id);
    }
}
