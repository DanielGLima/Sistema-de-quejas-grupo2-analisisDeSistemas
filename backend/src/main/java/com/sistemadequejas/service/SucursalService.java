package com.sistemadequejas.service;

import com.sistemadequejas.model.Sucursal;
import com.sistemadequejas.repository.SucursalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class SucursalService {

  @Autowired
  private SucursalRepository sucursalRepository;

  //find by all
  public List<Sucursal> findAll(){
    return sucursalRepository.findAll();
  }

  //find by Id
  public Optional<Sucursal>  findById(Integer idSucursal){
    return sucursalRepository.findById(idSucursal);
  }

  //save
  public Sucursal save(Sucursal sucursal){
    return sucursalRepository.save(sucursal);
  }

  //delete by Id
public void deleteById(Integer idSucursal){
    sucursalRepository.deleteById(idSucursal);
}
}
