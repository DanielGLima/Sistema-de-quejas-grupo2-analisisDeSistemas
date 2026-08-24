package com.sistemadequejas.service;

import com.sistemadequejas.model.EvidenciaCaso;
import com.sistemadequejas.repository.EvidenciaCasoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EvidenciaCasoService {

    @Autowired
    private EvidenciaCasoRepository evidenciaCasoRepository;

    public EvidenciaCaso save(EvidenciaCaso evidenciaCaso) {
        return evidenciaCasoRepository.save(evidenciaCaso);
    }
}
