package com.sistemadequejas.controller;

import com.sistemadequejas.model.Sucursal;
import com.sistemadequejas.service.SucursalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sucursales")
public class SucursalController {

  @Autowired
  private SucursalService sucursalService;

  @GetMapping
  public List<Sucursal> findAll(){
    return sucursalService.findAll();
  }

  @GetMapping("/{id}")
  public ResponseEntity<Sucursal> findById(@PathVariable Integer id){
    return sucursalService.findById(id)
      .map(ResponseEntity::ok)
      .orElseGet(() -> ResponseEntity.notFound().build());
  }

  @PostMapping
  public Sucursal create(@RequestBody Sucursal sucursal){
    return sucursalService.save(sucursal);
  }

  @PutMapping("/{id}")
  public ResponseEntity<Sucursal> update(@PathVariable Integer id, @RequestBody Sucursal sucursal) {
    if (sucursalService.findById(id).isEmpty()) {
      return ResponseEntity.notFound().build();
    }
    sucursal.setIdSucursal(id);
    return ResponseEntity.ok(sucursalService.save(sucursal));
  }


    @DeleteMapping("/{id}")
      public ResponseEntity<Void> delete(@PathVariable Integer id){
      if (sucursalService.findById(id).isEmpty()){
        return ResponseEntity.notFound().build();
      }
      sucursalService.deleteById(id);
      return ResponseEntity.noContent().build();
    }
}
