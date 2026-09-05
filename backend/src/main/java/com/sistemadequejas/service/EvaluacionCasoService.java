package com.sistemadequejas.service;

import com.sistemadequejas.model.EvaluacionCaso;
import com.sistemadequejas.repository.EvaluacionCasoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EvaluacionCasoService {

    @Autowired
    private EvaluacionCasoRepository evaluacionCasoRepository;

    public EvaluacionCaso save(EvaluacionCaso evaluacionCaso) {
        return evaluacionCasoRepository.save(evaluacionCaso);
    }
}
