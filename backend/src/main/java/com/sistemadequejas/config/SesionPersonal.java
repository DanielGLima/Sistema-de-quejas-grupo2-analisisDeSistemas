package com.sistemadequejas.config;

import com.sistemadequejas.model.Personal;
import com.sistemadequejas.service.PersonalService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;

// Sesion del personal interno (separada de la sesion del cliente, "idUsuario").
@Component
public class SesionPersonal {

    public static final String SESSION_ID_PERSONAL = "idPersonal";
    public static final String SIN_PERMISOS = "No posee permisos suficientes para realizar la acción";

    @Autowired
    private PersonalService personalService;

    public Personal requerir(HttpSession session) {
        Object idPersonal = session.getAttribute(SESSION_ID_PERSONAL);
        if (idPersonal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Debe iniciar sesión");
        }
        return personalService.findById((Integer) idPersonal)
                .filter(personal -> Boolean.TRUE.equals(personal.getActivo()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Debe iniciar sesión"));
    }

    // CU-10 FA02 / CU-13 FA04: rechaza si el rol no esta entre los permitidos.
    public Personal requerirRol(HttpSession session, String... rolesPermitidos) {
        Personal personal = requerir(session);
        boolean permitido = Arrays.asList(rolesPermitidos).contains(personal.getRol().getNombre());
        if (!permitido) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, SIN_PERMISOS);
        }
        return personal;
    }
}
