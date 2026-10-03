package com.sistemadequejas.controller;

import com.sistemadequejas.config.SesionPersonal;
import com.sistemadequejas.dto.PersonalRequest;
import com.sistemadequejas.model.Personal;
import com.sistemadequejas.service.BitacoraAuditoriaService;
import com.sistemadequejas.service.PersonalService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

// CU-13: administracion de usuarios internos (solo Administrador General).
@RestController
@RequestMapping("/api/admin/personal")
public class AdminPersonalController {

    @Autowired
    private PersonalService personalService;

    @Autowired
    private SesionPersonal sesionPersonal;

    @Autowired
    private BitacoraAuditoriaService bitacoraAuditoriaService;

    // Empleados que pueden recibir casos (para los selectores de CU-10).
    @GetMapping("/asignables")
    public List<Personal> asignables(HttpSession session) {
        sesionPersonal.requerirRol(session,
                PersonalService.ROL_ADMINISTRADOR, PersonalService.ROL_GERENTE, PersonalService.ROL_OPERADOR);
        return personalService.findActivos();
    }

    @GetMapping
    public List<Personal> listar(HttpSession session) {
        sesionPersonal.requerirRol(session, PersonalService.ROL_ADMINISTRADOR);
        return personalService.findAll();
    }

    @PostMapping
    public ResponseEntity<Personal> crear(@RequestBody PersonalRequest request, HttpSession session) {
        Personal actor = sesionPersonal.requerirRol(session, PersonalService.ROL_ADMINISTRADOR);
        Personal creado = personalService.crear(request.nombreCompleto(), request.correo(), request.idRol(),
                request.idSucursal(), request.activo(), request.passwordTemporal());

        bitacoraAuditoriaService.registrarAccionPersonal(actor, null, "Alta de usuario interno", "CU-13",
                BitacoraAuditoriaService.json("correo", creado.getCorreo(), "rol", creado.getRol().getNombre(),
                        "sucursal", creado.getSucursal().getNombreSucursal()));
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @PutMapping("/{id}")
    public Personal actualizar(@PathVariable Integer id, @RequestBody PersonalRequest request, HttpSession session) {
        Personal actor = sesionPersonal.requerirRol(session, PersonalService.ROL_ADMINISTRADOR);
        Personal personal = personalService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario interno no encontrado"));

        String anterior = BitacoraAuditoriaService.json("correo", personal.getCorreo(), "rol", personal.getRol().getNombre(),
                "activo", String.valueOf(personal.getActivo()));
        Personal actualizado = personalService.actualizar(personal, request.nombreCompleto(), request.correo(), request.idRol(),
                request.idSucursal(), request.activo(), request.passwordTemporal());

        bitacoraAuditoriaService.registrarAccionPersonal(actor, null, "Modificacion de usuario interno", "CU-13",
                "{\"anterior\":" + anterior + ",\"nuevo\":" + BitacoraAuditoriaService.json("correo", actualizado.getCorreo(),
                        "rol", actualizado.getRol().getNombre(), "activo", String.valueOf(actualizado.getActivo())) + "}");
        return actualizado;
    }
}
