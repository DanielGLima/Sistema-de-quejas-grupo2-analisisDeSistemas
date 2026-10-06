package com.sistemadequejas.dto;

// CU-13: alta/edicion de usuario interno.
public record PersonalRequest(String nombreCompleto, String correo, Integer idRol, Integer idSucursal, Boolean activo, String passwordTemporal) {
}
