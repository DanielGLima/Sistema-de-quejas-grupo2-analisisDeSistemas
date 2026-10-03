package com.sistemadequejas.service;

import com.sistemadequejas.model.RecuperacionContrasena;
import com.sistemadequejas.model.Usuario;
import com.sistemadequejas.repository.RecuperacionContrasenaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class RecuperacionContrasenaService {

    private static final String CARACTERES = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int LONGITUD_CODIGO = 6;
    // CU-02, requerimiento no funcional "Expiracion": ej. 15 minutos.
    private static final int MINUTOS_EXPIRACION = 15;

    private final SecureRandom random = new SecureRandom();

    @Autowired
    private RecuperacionContrasenaRepository recuperacionContrasenaRepository;

    // CU-02: genera un codigo de 6 caracteres con expiracion.
    public RecuperacionContrasena generarCodigo(Usuario usuario) {
        StringBuilder codigo = new StringBuilder(LONGITUD_CODIGO);
        for (int i = 0; i < LONGITUD_CODIGO; i++) {
            codigo.append(CARACTERES.charAt(random.nextInt(CARACTERES.length())));
        }

        RecuperacionContrasena recuperacion = new RecuperacionContrasena();
        recuperacion.setUsuario(usuario);
        recuperacion.setCodigoToken(codigo.toString());
        recuperacion.setFechaExpiracion(LocalDateTime.now().plusMinutes(MINUTOS_EXPIRACION));
        return recuperacionContrasenaRepository.save(recuperacion);
    }

    // CU-02: valida el codigo ingresado (sin consumirlo).
    // FA05 (codigo incorrecto) y FA06 (codigo expirado) son mensajes distintos.
    public RecuperacionContrasena validarCodigo(Usuario usuario, String codigo) {
        RecuperacionContrasena recuperacion = recuperacionContrasenaRepository
                .findFirstByUsuarioAndCodigoTokenAndUsadoFalseOrderByFechaCreacionDesc(usuario, codigo == null ? "" : codigo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "El código de recuperación ingresado es incorrecto"));

        if (recuperacion.getFechaExpiracion().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El código de recuperación ha expirado. Solicite uno nuevo");
        }
        return recuperacion;
    }

    // Marca el codigo como usado una vez que la contrasena fue restablecida.
    public void consumir(RecuperacionContrasena recuperacion) {
        recuperacion.setUsado(true);
        recuperacionContrasenaRepository.save(recuperacion);
    }
}
