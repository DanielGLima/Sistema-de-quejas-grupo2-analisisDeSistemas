package com.sistemadequejas.controller;

import com.sistemadequejas.model.Sucursal;
import com.sistemadequejas.service.SucursalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Solo lectura publica (sucursales activas). La administracion es CU-13 (/api/admin/catalogos).
@RestController
@RequestMapping("/api/sucursales")
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")

public class SucursalController {

  @Autowired
  private SucursalService sucursalService;

  @GetMapping
  public List<Sucursal> findAll(){
    return sucursalService.findAll().stream().filter(Sucursal::isActivo).toList();
  }

  @GetMapping("/{id}")
  public ResponseEntity<Sucursal> findById(@PathVariable Integer id){
    return sucursalService.findById(id)
      .map(ResponseEntity::ok)
      .orElseGet(() -> ResponseEntity.notFound().build());
  }
}
