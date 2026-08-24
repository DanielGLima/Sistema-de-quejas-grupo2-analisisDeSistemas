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
    private static final int MINUTOS_EXPIRACION = 20;

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

    // CU-02: valida el codigo ingresado y lo marca como usado.
    public void validarYConsumirCodigo(Usuario usuario, String codigo) {
        RecuperacionContrasena recuperacion = recuperacionContrasenaRepository
                .findFirstByUsuarioAndCodigoTokenAndUsadoFalseOrderByFechaCreacionDesc(usuario, codigo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "El codigo o enlace ha expirado"));

        if (recuperacion.getFechaExpiracion().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El codigo o enlace ha expirado");
        }

        recuperacion.setUsado(true);
        recuperacionContrasenaRepository.save(recuperacion);
    }
}
