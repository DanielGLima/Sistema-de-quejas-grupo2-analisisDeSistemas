package com.sistemadequejas.service;

import com.sistemadequejas.model.CategoriaCaso;
import com.sistemadequejas.repository.CategoriaCasoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CategoriaCasoService {

    @Autowired
    private CategoriaCasoRepository categoriaCasoRepository;

    public List<CategoriaCaso> findAll() {
        return categoriaCasoRepository.findAll();
    }

    public Optional<CategoriaCaso> findById(Integer id) {
        return categoriaCasoRepository.findById(id);
    }

    public CategoriaCaso save(CategoriaCaso categoriaCaso) {
        return categoriaCasoRepository.save(categoriaCaso);
    }

    public void deleteById(Integer id) {
        categoriaCasoRepository.deleteById(id);
    }
}
