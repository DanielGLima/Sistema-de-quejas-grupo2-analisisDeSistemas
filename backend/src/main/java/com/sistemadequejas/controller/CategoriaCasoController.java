package com.sistemadequejas.controller;

import com.sistemadequejas.model.CategoriaCaso;
import com.sistemadequejas.service.CategoriaCasoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categorias-caso")
public class CategoriaCasoController {

    @Autowired
    private CategoriaCasoService categoriaCasoService;

    @GetMapping
    public List<CategoriaCaso> findAll() {
        return categoriaCasoService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoriaCaso> findById(@PathVariable Integer id) {
        return categoriaCasoService.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public CategoriaCaso create(@RequestBody CategoriaCaso categoriaCaso) {
        return categoriaCasoService.save(categoriaCaso);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoriaCaso> update(@PathVariable Integer id, @RequestBody CategoriaCaso categoriaCaso) {
        if (categoriaCasoService.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        categoriaCaso.setIdCategoria(id);
        return ResponseEntity.ok(categoriaCasoService.save(categoriaCaso));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        if (categoriaCasoService.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        categoriaCasoService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
