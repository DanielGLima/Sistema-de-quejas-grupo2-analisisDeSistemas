package com.sistemadequejas.controller;

import com.sistemadequejas.model.Evidencia;
import com.sistemadequejas.service.EvidenciaService;
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
@RequestMapping("/api/evidencias")
public class EvidenciaController {

    @Autowired
    private EvidenciaService evidenciaService;

    @GetMapping
    public List<Evidencia> findAll() {
        return evidenciaService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Evidencia> findById(@PathVariable Integer id) {
        return evidenciaService.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public Evidencia create(@RequestBody Evidencia evidencia) {
        return evidenciaService.save(evidencia);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Evidencia> update(@PathVariable Integer id, @RequestBody Evidencia evidencia) {
        if (evidenciaService.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        evidencia.setIdEvidencia(id);
        return ResponseEntity.ok(evidenciaService.save(evidencia));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        if (evidenciaService.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        evidenciaService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
