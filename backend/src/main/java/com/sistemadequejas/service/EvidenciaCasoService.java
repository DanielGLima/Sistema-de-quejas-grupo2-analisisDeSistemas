package com.sistemadequejas.service;

import com.sistemadequejas.model.Caso;
import com.sistemadequejas.model.EvidenciaCaso;
import com.sistemadequejas.repository.EvidenciaCasoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EvidenciaCasoService {

    @Autowired
    private EvidenciaCasoRepository evidenciaCasoRepository;

    public EvidenciaCaso save(EvidenciaCaso evidenciaCaso) {
        return evidenciaCasoRepository.save(evidenciaCaso);
    }

    public List<EvidenciaCaso> findByCaso(Caso caso) {
        return evidenciaCasoRepository.findByCaso(caso);
    }
}
