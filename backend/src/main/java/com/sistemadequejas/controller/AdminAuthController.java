package com.sistemadequejas.controller;

import com.sistemadequejas.config.SesionPersonal;
import com.sistemadequejas.dto.LoginRequest;
import com.sistemadequejas.model.Personal;
import com.sistemadequejas.service.BitacoraAuditoriaService;
import com.sistemadequejas.service.PersonalService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Acceso del personal interno (Administrador General, Gerente, Operador).
@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {

    @Autowired
    private PersonalService personalService;

    @Autowired
    private SesionPersonal sesionPersonal;

    @Autowired
    private BitacoraAuditoriaService bitacoraAuditoriaService;

    @PostMapping("/login")
    public ResponseEntity<Personal> login(@RequestBody LoginRequest request, HttpSession session) {
        Personal personal = personalService.autenticar(request.getCorreo(), request.getPassword());
        session.setAttribute(SesionPersonal.SESSION_ID_PERSONAL, personal.getIdPersonal());
        bitacoraAuditoriaService.registrarAccionPersonal(personal, null, "Inicio de sesion", "ACCESO",
                BitacoraAuditoriaService.json("rol", personal.getRol().getNombre()));
        return ResponseEntity.ok(personal);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<Personal> me(HttpSession session) {
        return ResponseEntity.ok(sesionPersonal.requerir(session));
    }
}
