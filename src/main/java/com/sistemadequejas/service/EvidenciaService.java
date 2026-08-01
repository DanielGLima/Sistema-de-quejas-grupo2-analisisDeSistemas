package com.sistemadequejas.service;

import com.sistemadequejas.model.Evidencia;
import com.sistemadequejas.repository.EvidenciaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class EvidenciaService {

    @Autowired
    private EvidenciaRepository evidenciaRepository;

    public List<Evidencia> findAll() {
        return evidenciaRepository.findAll();
    }

    public Optional<Evidencia> findById(Integer id) {
        return evidenciaRepository.findById(id);
    }

    public Evidencia save(Evidencia evidencia) {
        return evidenciaRepository.save(evidencia);
    }

    public void deleteById(Integer id) {
        evidenciaRepository.deleteById(id);
    }
}
