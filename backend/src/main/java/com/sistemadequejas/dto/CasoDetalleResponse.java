package com.sistemadequejas.dto;

import com.sistemadequejas.model.Caso;
import com.sistemadequejas.model.EvidenciaCaso;
import com.sistemadequejas.model.HistorialEstadoCaso;
import com.sistemadequejas.model.RespuestaCaso;

import java.util.List;

// CU-06, paso 5: detalle completo del caso con sus evidencias e historial de estados.
public class CasoDetalleResponse {

    private final Caso caso;
    private final List<EvidenciaCaso> evidencias;
    private final List<HistorialEstadoCaso> historial;
    private final List<RespuestaCaso> respuestas;

    public CasoDetalleResponse(Caso caso, List<EvidenciaCaso> evidencias, List<HistorialEstadoCaso> historial, List<RespuestaCaso> respuestas) {
        this.caso = caso;
        this.evidencias = evidencias;
        this.historial = historial;
        this.respuestas = respuestas;
    }

    public Caso getCaso() {
        return caso;
    }

    public List<EvidenciaCaso> getEvidencias() {
        return evidencias;
    }

    public List<HistorialEstadoCaso> getHistorial() {
        return historial;
    }

    public List<RespuestaCaso> getRespuestas() {
        return respuestas;
    }
}
