package com.sistemadequejas.dto;

// CU-13: alta/edicion de un elemento de catalogo. Segun el catalogo se usan solo algunos campos.
public record CatalogoRequest(String nombre, String direccion, String telefono, String codigo, Boolean activo) {
}
