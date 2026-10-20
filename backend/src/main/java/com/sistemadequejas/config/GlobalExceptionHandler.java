package com.sistemadequejas.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

// Controla explicitamente el cuerpo JSON de los errores de negocio.
//
// Por defecto Spring Boot no expone el texto de un ResponseStatusException
// (ej. "Correo no registrado", "Contrasena actual incorrecta") en la
// respuesta: segun la version, lo esconde del todo o lo mueve a un formato
// ProblemDetail (campo "detail" en vez de "message"). El frontend Angular ya
// esta escrito para leer err.error?.message en todos los formularios, asi
// que este manejador global garantiza ese contrato sin depender del
// comportamiento por defecto de Spring Boot.
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> manejarResponseStatusException(ResponseStatusException ex) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("status", ex.getStatusCode().value());
        cuerpo.put("message", ex.getReason() != null ? ex.getReason() : "Ocurrio un error inesperado");

        return ResponseEntity.status(ex.getStatusCode()).body(cuerpo);
    }

    // Cuerpo con un tipo o formato que no se puede leer (por ejemplo una fecha mal escrita): el backend
    // es quien decide el mensaje, el frontend no valida formatos.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> manejarCuerpoNoLegible(HttpMessageNotReadableException ex) {
        String detalle = String.valueOf(ex.getMessage());
        String mensaje = detalle.contains("LocalDate")
                ? "Formato de datos no válido. Revise el campo: fecha de nacimiento (formato dd/mm/aaaa, no puede ser posterior a hoy ni anterior a 1900)"
                : "Los datos enviados no tienen un formato válido";

        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("status", HttpStatus.BAD_REQUEST.value());
        cuerpo.put("message", mensaje);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(cuerpo);
    }
}
