package com.sistemadequejas.controller;

import com.sistemadequejas.model.HistorialEstado;
import com.sistemadequejas.service.HistorialEstadoService;
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
@RequestMapping("/api/historial-estados")
public class HistorialEstadoController {

    @Autowired
    private HistorialEstadoService historialEstadoService;

    @GetMapping
    public List<HistorialEstado> findAll() {
        return historialEstadoService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<HistorialEstado> findById(@PathVariable Integer id) {
        return historialEstadoService.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public HistorialEstado create(@RequestBody HistorialEstado historialEstado) {
        return historialEstadoService.save(historialEstado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<HistorialEstado> update(@PathVariable Integer id, @RequestBody HistorialEstado historialEstado) {
        if (historialEstadoService.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        historialEstado.setIdHistorial(id);
        return ResponseEntity.ok(historialEstadoService.save(historialEstado));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        if (historialEstadoService.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        historialEstadoService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
