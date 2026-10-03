package com.sistemadequejas.controller;

import com.sistemadequejas.model.CategoriaCaso;
import com.sistemadequejas.service.CategoriaCasoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Solo lectura publica (categorias activas). La administracion es CU-13 (/api/admin/catalogos).
@RestController
@RequestMapping("/api/categorias-caso")
public class CategoriaCasoController {

    @Autowired
    private CategoriaCasoService categoriaCasoService;

    @GetMapping
    public List<CategoriaCaso> findAll() {
        return categoriaCasoService.findAll().stream().filter(c -> Boolean.TRUE.equals(c.getActivo())).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoriaCaso> findById(@PathVariable Integer id) {
        return categoriaCasoService.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
