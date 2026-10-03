package com.sistemadequejas.dto;

// CU-15: registro de auditoria listo para mostrar.
public record BitacoraResponse(Integer idBitacora, String usuarioId, String rolUsuario, String tipoAccion, String moduloAfectado,
                               String direccionIp, String fechaHoraExacta, String identificadorCaso, String detalle) {
}
