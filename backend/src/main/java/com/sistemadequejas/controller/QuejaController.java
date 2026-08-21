package com.sistemadequejas.controller;

import com.sistemadequejas.model.Queja;
import com.sistemadequejas.service.QuejaService;
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
@RequestMapping("/api/quejas")
public class QuejaController {

    @Autowired
    private QuejaService quejaService;

    @GetMapping
    public List<Queja> findAll() {
        return quejaService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Queja> findById(@PathVariable Integer id) {
        return quejaService.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public Queja create(@RequestBody Queja queja) {
        return quejaService.save(queja);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Queja> update(@PathVariable Integer id, @RequestBody Queja queja) {
        if (quejaService.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        queja.setIdQueja(id);
        return ResponseEntity.ok(quejaService.save(queja));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        if (quejaService.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        quejaService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
