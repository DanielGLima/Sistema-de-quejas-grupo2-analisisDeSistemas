package com.sistemadequejas.controller;

import com.sistemadequejas.model.EstadoCaso;
import com.sistemadequejas.service.EstadoCasoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Solo lectura. La administracion es CU-13 (/api/admin/catalogos).
@RestController
@RequestMapping("/api/estados-caso")
public class EstadoCasoController {

    @Autowired
    private EstadoCasoService estadoCasoService;

    @GetMapping
    public List<EstadoCaso> findAll() {
        return estadoCasoService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<EstadoCaso> findById(@PathVariable Integer id) {
        return estadoCasoService.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
