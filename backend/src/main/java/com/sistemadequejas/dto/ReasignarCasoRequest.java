package com.sistemadequejas.dto;

import java.time.LocalDateTime;

public record ReasignarCasoRequest(Integer idPersonalNuevo, String motivo, LocalDateTime fechaActualizacion) {
}
