package com.sistemadequejas.config;

import com.sistemadequejas.service.PersonalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

// Crea la primera cuenta de Administrador General si la tabla personal esta vacia.
// La contrasena viene de la variable de entorno ADMIN_PASSWORD (nunca del repositorio).
@Component
public class AdministradorInicial implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdministradorInicial.class);

    @Autowired
    private PersonalService personalService;

    @Value("${app.admin.correo}")
    private String correo;

    @Value("${app.admin.nombre}")
    private String nombre;

    @Value("${app.admin.password:}")
    private String password;

    @Override
    public void run(String... args) {
        if (personalService.contar() > 0) {
            return;
        }
        if (password == null || password.isBlank()) {
            log.warn("No existe personal interno y ADMIN_PASSWORD no esta definida: no se creo el Administrador General inicial.");
            return;
        }
        personalService.crearAdministradorInicial(nombre, correo, password);
        log.info("Administrador General inicial creado: {}", correo);
    }
}
