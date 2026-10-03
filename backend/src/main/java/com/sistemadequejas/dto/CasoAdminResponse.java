package com.sistemadequejas.dto;

import com.sistemadequejas.model.EvidenciaCaso;
import com.sistemadequejas.model.HistorialEstadoCaso;
import com.sistemadequejas.model.ReasignacionCaso;
import com.sistemadequejas.model.RespuestaCaso;

import java.time.LocalDateTime;
import java.util.List;

// CU-10/11/12: caso tal como lo ve el personal interno.
public record CasoAdminResponse(
        Integer idCaso,
        String identificadorVisible,
        String tipoCaso,
        String categoria,
        Integer idSucursal,
        String sucursal,
        String estado,
        String descripcion,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion,
        boolean esAnonimo,
        String nombreCliente,
        String correoCliente,
        Integer idPersonalAsignado,
        String personalAsignado,
        String motivoReapertura,
        List<String> estadosPermitidos,
        List<RespuestaCaso> respuestas,
        List<ReasignacionCaso> reasignaciones,
        List<EvidenciaCaso> evidencias,
        List<HistorialEstadoCaso> historial) {
}
