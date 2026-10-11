package com.sistemadequejas.dto;

public class LoginResponse {
  private Integer id;
  private String nombreCompleto;
  private String correo;
  private String rol; // "CLIENTE", "OPERADOR", "GERENTE", "ADMINISTRADOR"
  private String tipoUsuario; // "CLIENTE" o "PERSONAL"

  public LoginResponse() {}

  public LoginResponse(Integer id, String nombreCompleto, String correo, String rol, String tipoUsuario) {
    this.id = id;
    this.nombreCompleto = nombreCompleto;
    this.correo = correo;
    this.rol = rol;
    this.tipoUsuario = tipoUsuario;
  }

  public Integer getId() { return id; }
  public void setId(Integer id) { this.id = id; }

  public String getNombreCompleto() { return nombreCompleto; }
  public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }

  public String getCorreo() { return correo; }
  public void setCorreo(String correo) { this.correo = correo; }

  public String getRol() { return rol; }
  public void setRol(String rol) { this.rol = rol; }

  public String getTipoUsuario() { return tipoUsuario; }
  public void setTipoUsuario(String tipoUsuario) { this.tipoUsuario = tipoUsuario; }
}
