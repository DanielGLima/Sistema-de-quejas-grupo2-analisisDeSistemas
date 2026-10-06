package com.sistemadequejas.controller;

import com.sistemadequejas.model.TipoCaso;
import com.sistemadequejas.service.TipoCasoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Solo lectura publica (tipos activos). La administracion es CU-13 (/api/admin/catalogos).
@RestController
@RequestMapping("/api/tipos-caso")
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")

public class TipoCasoController {

    @Autowired
    private TipoCasoService tipoCasoService;

    @GetMapping
    public List<TipoCaso> findAll() {
        return tipoCasoService.findAll().stream().filter(t -> Boolean.TRUE.equals(t.getActivo())).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<TipoCaso> findById(@PathVariable Integer id) {
        return tipoCasoService.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
