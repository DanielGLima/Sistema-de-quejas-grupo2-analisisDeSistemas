package com.sistemadequejas.controller;

import com.sistemadequejas.model.TipoCaso;
import com.sistemadequejas.service.TipoCasoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tipos-caso")
public class TipoCasoController {

    @Autowired
    private TipoCasoService tipoCasoService;

    @GetMapping
    public List<TipoCaso> findAll() {
        return tipoCasoService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<TipoCaso> findById(@PathVariable Integer id) {
        return tipoCasoService.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public TipoCaso create(@RequestBody TipoCaso tipoCaso) {
        return tipoCasoService.save(tipoCaso);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TipoCaso> update(@PathVariable Integer id, @RequestBody TipoCaso tipoCaso) {
        if (tipoCasoService.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        tipoCaso.setIdTipoCaso(id);
        return ResponseEntity.ok(tipoCasoService.save(tipoCaso));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        if (tipoCasoService.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        tipoCasoService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
