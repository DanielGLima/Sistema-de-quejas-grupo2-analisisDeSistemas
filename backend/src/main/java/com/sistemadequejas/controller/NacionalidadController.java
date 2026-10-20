package com.sistemadequejas.controller;

import com.sistemadequejas.model.Nacionalidad;
import com.sistemadequejas.repository.NacionalidadRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Solo lectura publica (nacionalidades activas). Se mantiene directamente en la base de datos.
@RestController
@RequestMapping("/api/nacionalidades")
public class NacionalidadController {

    @Autowired
    private NacionalidadRepository nacionalidadRepository;

    @GetMapping
    public List<Nacionalidad> findAll() {
        return nacionalidadRepository.findByActivoTrueOrderByIdNacionalidad();
    }
}
