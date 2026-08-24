package com.sistemadequejas.model;

import jakarta.persistence.*;

@Entity
@Table(name = "sucursal")
public class Sucursal {

  @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Integer idSucursal;

  @Column(name = "nombre", length = 50, nullable = false)
  private String nombreSucursal;

  @Column(name = "direccion", length = 150, nullable = false)
  private String direccionSucursal;

  @Column(name = "telefono" , length = 20)
  private String telefonoSucursal;

  @Column(name = "activo", nullable = false)
  private boolean activo = true;

  public Integer getIdSucursal() {
    return idSucursal;
  }

  public void setIdSucursal(Integer idSucursal) {
    this.idSucursal = idSucursal;
  }

  public String getNombreSucursal() {
    return nombreSucursal;
  }

  public void setNombreSucursal(String nombreSucursal) {
    this.nombreSucursal = nombreSucursal;
  }

  public String getDireccionSucursal() {
    return direccionSucursal;
  }

  public void setDireccionSucursal(String direccionSucursal) {
    this.direccionSucursal = direccionSucursal;
  }

  public String getTelefonoSucursal() {
    return telefonoSucursal;
  }

  public void setTelefonoSucursal(String telefonoSucursal) {
    this.telefonoSucursal = telefonoSucursal;
  }

  public boolean isActivo() {
    return activo;
  }

  public void setActivo(boolean activo) {
    this.activo = activo;
  }
}
