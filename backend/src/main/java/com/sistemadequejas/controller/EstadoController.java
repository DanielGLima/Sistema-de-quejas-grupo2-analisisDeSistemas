package com.sistemadequejas.controller;

import com.sistemadequejas.model.Estado;
import com.sistemadequejas.service.EstadoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/estados")
public class EstadoController {

    @Autowired
    private EstadoService estadoService;

    @GetMapping
    public List<Estado> findAll() {
        return estadoService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Estado> findById(@PathVariable Integer id) {
        return estadoService.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public Estado create(@RequestBody Estado estado) {
        return estadoService.save(estado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Estado> update(@PathVariable Integer id, @RequestBody Estado estado) {
        if (estadoService.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        estado.setIdEstado(id);
        return ResponseEntity.ok(estadoService.save(estado));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        if (estadoService.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        estadoService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
