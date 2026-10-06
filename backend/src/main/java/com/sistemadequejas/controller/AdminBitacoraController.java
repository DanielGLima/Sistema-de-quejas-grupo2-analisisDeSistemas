package com.sistemadequejas.controller;

import com.sistemadequejas.config.SesionPersonal;
import com.sistemadequejas.dto.BitacoraResponse;
import com.sistemadequejas.model.BitacoraAuditoria;
import com.sistemadequejas.model.Notificacion;
import com.sistemadequejas.model.Personal;
import com.sistemadequejas.service.BitacoraAuditoriaService;
import com.sistemadequejas.service.NotificacionService;
import com.sistemadequejas.service.PersonalService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// CU-15 (consulta de la bitacora) y CU-14 (historial de notificaciones): solo lectura.
@RestController
@RequestMapping("/api/admin")
public class AdminBitacoraController {

    private static final DateTimeFormatter FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    @Autowired
    private BitacoraAuditoriaService bitacoraAuditoriaService;

    @Autowired
    private NotificacionService notificacionService;

    @Autowired
    private PersonalService personalService;

    @Autowired
    private SesionPersonal sesionPersonal;

    @GetMapping("/bitacora")
    public List<BitacoraResponse> bitacora(
            @RequestParam(required = false) String fechaInicial,
            @RequestParam(required = false) String fechaFinal,
            @RequestParam(required = false) String accion,
            @RequestParam(required = false) String modulo,
            @RequestParam(required = false) String tipoActor,
            @RequestParam(defaultValue = "200") int limite,
            HttpSession session) {

        sesionPersonal.requerirRol(session, PersonalService.ROL_ADMINISTRADOR);

        LocalDate desde = AdminCasoController.parsearFecha(fechaInicial, "inicial");
        LocalDate hasta = AdminCasoController.parsearFecha(fechaFinal, "final");

        Map<Integer, Personal> cachePersonal = new HashMap<>();
        return bitacoraAuditoriaService.consultar(
                        desde == null ? null : desde.atStartOfDay(),
                        hasta == null ? null : hasta.atTime(LocalTime.MAX),
                        accion, modulo, tipoActor, limite)
                .stream()
                .map(registro -> aRespuesta(registro, cachePersonal))
                .toList();
    }

    @GetMapping("/notificaciones")
    public List<Notificacion> notificaciones(HttpSession session) {
        sesionPersonal.requerirRol(session, PersonalService.ROL_ADMINISTRADOR, PersonalService.ROL_GERENTE);
        return notificacionService.historial();
    }

    private BitacoraResponse aRespuesta(BitacoraAuditoria registro, Map<Integer, Personal> cachePersonal) {
        String usuarioId;
        String rol;
        if ("Personal".equals(registro.getTipoActor()) && registro.getIdPersonal() != null) {
            Personal personal = cachePersonal.computeIfAbsent(registro.getIdPersonal(),
                    id -> personalService.findById(id).orElse(null));
            usuarioId = personal == null ? "PER-" + registro.getIdPersonal()
                    : "PER-" + personal.getIdPersonal() + " (" + personal.getNombreCompleto() + ")";
            rol = personal == null ? "Personal" : personal.getRol().getNombre();
        } else if (registro.getUsuario() != null) {
            usuarioId = "USR-" + registro.getUsuario().getIdUsuario() + " (" + registro.getUsuario().getNombreCompleto() + ")";
            rol = "Usuario";
        } else {
            usuarioId = "SISTEMA";
            rol = "Sistema";
        }

        return new BitacoraResponse(
                registro.getIdBitacora(),
                usuarioId,
                rol,
                registro.getAccion(),
                registro.getModuloAfectado(),
                registro.getIpOrigen(),
                registro.getFechaHora().format(FORMATO_FECHA_HORA),
                registro.getCaso() == null ? null : registro.getCaso().getIdentificadorVisible(),
                registro.getDetalle());
    }
}
