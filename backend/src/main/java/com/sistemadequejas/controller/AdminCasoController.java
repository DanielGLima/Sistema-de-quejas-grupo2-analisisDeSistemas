package com.sistemadequejas.controller;

import com.sistemadequejas.config.SesionPersonal;
import com.sistemadequejas.dto.CasoAdminResponse;
import com.sistemadequejas.dto.GestionarCasoRequest;
import com.sistemadequejas.dto.ReasignarCasoRequest;
import com.sistemadequejas.dto.ResponderCasoRequest;
import com.sistemadequejas.model.Caso;
import com.sistemadequejas.model.Personal;
import com.sistemadequejas.model.RespuestaCaso;
import com.sistemadequejas.service.CasoService;
import com.sistemadequejas.service.PersonalService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

// CU-10 (gestionar), CU-11 (responder) y CU-12 (buscar/filtrar) para el personal interno.
@RestController
@RequestMapping("/api/admin/casos")
public class AdminCasoController {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Autowired
    private CasoService casoService;

    @Autowired
    private SesionPersonal sesionPersonal;

    @Autowired
    private CasoAdminMapper mapper;

    // CU-12: busqueda con criterios combinables.
    @GetMapping
    public List<CasoAdminResponse> buscar(
            @RequestParam(required = false) String fechaInicial,
            @RequestParam(required = false) String fechaFinal,
            @RequestParam(required = false) Integer idTipoCaso,
            @RequestParam(required = false) Integer idCategoria,
            @RequestParam(required = false) Integer idSucursal,
            @RequestParam(required = false) Integer idEstado,
            @RequestParam(required = false) String identificador,
            @RequestParam(required = false) String correoCliente,
            @RequestParam(required = false) Integer idResponsable,
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) String ordenarPor,
            @RequestParam(defaultValue = "desc") String direccion,
            HttpSession session) {

        Personal actor = sesionPersonal.requerirRol(session,
                PersonalService.ROL_ADMINISTRADOR, PersonalService.ROL_GERENTE, PersonalService.ROL_OPERADOR);

        // FA02: criterios invalidos, indicando como corregirlos.
        LocalDate desde = parsearFecha(fechaInicial, "inicial");
        LocalDate hasta = parsearFecha(fechaFinal, "final");
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La fecha inicial no puede ser posterior a la fecha final (formato DD/MM/AAAA)");
        }
        if (identificador != null && identificador.length() > 20) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El código del caso admite máximo 20 caracteres");
        }
        if (correoCliente != null && correoCliente.length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El correo admite máximo 100 caracteres");
        }

        return casoService.buscar(
                        actor,
                        desde == null ? null : desde.atStartOfDay(),
                        hasta == null ? null : hasta.atTime(LocalTime.MAX),
                        idTipoCaso, idCategoria, idSucursal, idEstado, identificador, correoCliente,
                        idResponsable, texto, ordenarPor, "asc".equalsIgnoreCase(direccion))
                .stream()
                .map(caso -> mapper.toResponse(caso, actor, false))
                .toList();
    }

    @GetMapping("/{id}")
    public CasoAdminResponse detalle(@PathVariable Integer id, HttpSession session) {
        Personal actor = sesionPersonal.requerirRol(session,
                PersonalService.ROL_ADMINISTRADOR, PersonalService.ROL_GERENTE, PersonalService.ROL_OPERADOR);
        Caso caso = obtener(id);
        casoService.validarAlcance(actor, caso);
        return mapper.toResponse(caso, actor, true);
    }

    // CU-10
    @PutMapping("/{id}/gestion")
    public CasoAdminResponse gestionar(@PathVariable Integer id, @RequestBody GestionarCasoRequest request, HttpSession session) {
        Personal actor = sesionPersonal.requerirRol(session, PersonalService.ROL_ADMINISTRADOR, PersonalService.ROL_GERENTE);
        Caso caso = casoService.gestionar(obtener(id), actor, request.idPersonalAsignado(), request.nuevoEstado(),
                request.notas(), request.fechaActualizacion());
        return mapper.toResponse(caso, actor, true);
    }

    @PutMapping("/{id}/reasignar")
    public CasoAdminResponse reasignar(@PathVariable Integer id, @RequestBody ReasignarCasoRequest request, HttpSession session) {
        Personal actor = sesionPersonal.requerirRol(session, PersonalService.ROL_ADMINISTRADOR, PersonalService.ROL_GERENTE);
        Caso caso = casoService.reasignar(obtener(id), actor, request.idPersonalNuevo(), request.motivo(), request.fechaActualizacion());
        return mapper.toResponse(caso, actor, true);
    }

    // CU-11
    @PostMapping("/{id}/respuesta")
    public ResponseEntity<CasoAdminResponse> responder(@PathVariable Integer id, @RequestBody ResponderCasoRequest request, HttpSession session) {
        Personal actor = sesionPersonal.requerirRol(session,
                PersonalService.ROL_ADMINISTRADOR, PersonalService.ROL_GERENTE, PersonalService.ROL_OPERADOR);
        casoService.responder(obtener(id), actor, request.titulo(), request.cuerpo(), request.accionesSeguimiento());
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(obtener(id), actor, true));
    }

    // CU-11, FA01: el Gerente confirma la respuesta de un Operador.
    @PutMapping("/respuestas/{idRespuesta}/aprobar")
    public CasoAdminResponse aprobar(@PathVariable Integer idRespuesta, HttpSession session) {
        Personal actor = sesionPersonal.requerirRol(session, PersonalService.ROL_ADMINISTRADOR, PersonalService.ROL_GERENTE);
        RespuestaCaso respuesta = casoService.findRespuesta(idRespuesta)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Respuesta no encontrada"));
        casoService.aprobarRespuesta(respuesta, actor);
        return mapper.toResponse(obtener(respuesta.getCaso().getIdCaso()), actor, true);
    }

    private Caso obtener(Integer id) {
        return casoService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Caso no encontrado"));
    }

    // Acepta DD/MM/AAAA (formato del documento) o AAAA-MM-DD.
    static LocalDate parsearFecha(String texto, String etiqueta) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return texto.contains("/") ? LocalDate.parse(texto.trim(), FORMATO_FECHA) : LocalDate.parse(texto.trim());
        } catch (DateTimeParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La fecha " + etiqueta + " es inválida. Use el formato DD/MM/AAAA");
        }
    }
}
