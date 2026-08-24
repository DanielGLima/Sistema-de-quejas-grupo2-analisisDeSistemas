package com.sistemadequejas.service;

import com.sistemadequejas.model.TipoCaso;
import com.sistemadequejas.repository.TipoCasoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TipoCasoService {

    @Autowired
    private TipoCasoRepository tipoCasoRepository;

    public List<TipoCaso> findAll() {
        return tipoCasoRepository.findAll();
    }

    public Optional<TipoCaso> findById(Integer id) {
        return tipoCasoRepository.findById(id);
    }

    public TipoCaso save(TipoCaso tipoCaso) {
        return tipoCasoRepository.save(tipoCaso);
    }

    public void deleteById(Integer id) {
        tipoCasoRepository.deleteById(id);
    }
}
