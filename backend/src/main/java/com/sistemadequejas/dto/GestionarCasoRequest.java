package com.sistemadequejas.dto;

import java.time.LocalDateTime;

// CU-10: datos de gestion. fechaActualizacion es la que el cliente vio al cargar el caso (FA03).
public record GestionarCasoRequest(Integer idPersonalAsignado, String nuevoEstado, String notas, LocalDateTime fechaActualizacion) {
}
