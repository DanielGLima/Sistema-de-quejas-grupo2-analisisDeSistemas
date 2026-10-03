package com.sistemadequejas.controller;

import com.sistemadequejas.dto.CasoAdminResponse;
import com.sistemadequejas.model.Caso;
import com.sistemadequejas.model.Personal;
import com.sistemadequejas.model.ReaperturaCaso;
import com.sistemadequejas.service.CasoService;
import com.sistemadequejas.service.EvidenciaCasoService;
import com.sistemadequejas.service.HistorialEstadoCasoService;
import com.sistemadequejas.service.PersonalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CasoAdminMapper {

    @Autowired
    private CasoService casoService;

    @Autowired
    private EvidenciaCasoService evidenciaCasoService;

    @Autowired
    private HistorialEstadoCasoService historialEstadoCasoService;

    public CasoAdminResponse toResponse(Caso caso, Personal actor, boolean conDetalle) {
        // Las denuncias anonimas ocultan los datos del cliente salvo al Administrador General.
        boolean ocultarCliente = Boolean.TRUE.equals(caso.getEsAnonimo())
                && !PersonalService.ROL_ADMINISTRADOR.equals(actor.getRol().getNombre());

        List<ReaperturaCaso> reaperturas = casoService.reaperturasDe(caso);
        String motivoReapertura = reaperturas.isEmpty() ? null : reaperturas.get(0).getMotivo();
        Personal asignado = caso.getPersonalAsignado();

        return new CasoAdminResponse(
                caso.getIdCaso(),
                caso.getIdentificadorVisible(),
                caso.getTipoCaso().getNombre(),
                caso.getCategoriaCaso() == null ? null : caso.getCategoriaCaso().getNombre(),
                caso.getSucursal().getIdSucursal(),
                caso.getSucursal().getNombreSucursal(),
                caso.getEstadoCaso().getNombre(),
                caso.getDescripcion(),
                caso.getFechaCreacion(),
                caso.getFechaActualizacion(),
                Boolean.TRUE.equals(caso.getEsAnonimo()),
                ocultarCliente ? null : caso.getUsuario().getNombreCompleto(),
                ocultarCliente ? null : caso.getUsuario().getCorreo(),
                asignado == null ? null : asignado.getIdPersonal(),
                asignado == null ? null : asignado.getNombreCompleto(),
                motivoReapertura,
                CasoService.estadosPermitidos(caso.getEstadoCaso().getNombre()),
                casoService.respuestasDe(caso),
                casoService.reasignacionesDe(caso),
                conDetalle ? evidenciaCasoService.findByCaso(caso) : null,
                conDetalle ? historialEstadoCasoService.findByCaso(caso) : null);
    }
}
