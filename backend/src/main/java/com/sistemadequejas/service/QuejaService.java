package com.sistemadequejas.service;

import com.sistemadequejas.model.Queja;
import com.sistemadequejas.repository.QuejaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class QuejaService {

    @Autowired
    private QuejaRepository quejaRepository;

    public List<Queja> findAll() {
        return quejaRepository.findAll();
    }

    public Optional<Queja> findById(Integer id) {
        return quejaRepository.findById(id);
    }

    public Queja save(Queja queja) {
        return quejaRepository.save(queja);
    }

    public void deleteById(Integer id) {
        quejaRepository.deleteById(id);
    }
}
