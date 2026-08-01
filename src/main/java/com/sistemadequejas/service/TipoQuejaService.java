package com.sistemadequejas.service;

import com.sistemadequejas.model.TipoQueja;
import com.sistemadequejas.repository.TipoQuejaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TipoQuejaService {

    @Autowired
    private TipoQuejaRepository tipoQuejaRepository;

    public List<TipoQueja> findAll() {
        return tipoQuejaRepository.findAll();
    }

    public Optional<TipoQueja> findById(Integer id) {
        return tipoQuejaRepository.findById(id);
    }

    public TipoQueja save(TipoQueja tipoQueja) {
        return tipoQuejaRepository.save(tipoQueja);
    }

    public void deleteById(Integer id) {
        tipoQuejaRepository.deleteById(id);
    }
}
