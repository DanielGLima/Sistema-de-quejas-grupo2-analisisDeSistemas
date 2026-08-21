package com.sistemadequejas.controller;

import com.sistemadequejas.model.TipoQueja;
import com.sistemadequejas.service.TipoQuejaService;
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
@RequestMapping("/api/tipos-queja")
public class TipoQuejaController {

    @Autowired
    private TipoQuejaService tipoQuejaService;

    @GetMapping
    public List<TipoQueja> findAll() {
        return tipoQuejaService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<TipoQueja> findById(@PathVariable Integer id) {
        return tipoQuejaService.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public TipoQueja create(@RequestBody TipoQueja tipoQueja) {
        return tipoQuejaService.save(tipoQueja);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TipoQueja> update(@PathVariable Integer id, @RequestBody TipoQueja tipoQueja) {
        if (tipoQuejaService.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        tipoQueja.setIdTipoQueja(id);
        return ResponseEntity.ok(tipoQuejaService.save(tipoQueja));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        if (tipoQuejaService.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        tipoQuejaService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
