package com.sistemadequejas.controller;

import com.sistemadequejas.model.EstadoCaso;
import com.sistemadequejas.service.EstadoCasoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @PostMapping
    public EstadoCaso create(@RequestBody EstadoCaso estadoCaso) {
        return estadoCasoService.save(estadoCaso);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EstadoCaso> update(@PathVariable Integer id, @RequestBody EstadoCaso estadoCaso) {
        if (estadoCasoService.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        estadoCaso.setIdEstado(id);
        return ResponseEntity.ok(estadoCasoService.save(estadoCaso));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        if (estadoCasoService.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        estadoCasoService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
