package com.sistemadequejas.controller;

import com.sistemadequejas.dto.ActualizarPerfilRequest;
import com.sistemadequejas.dto.LoginRequest;
import com.sistemadequejas.dto.RecuperarConfirmarRequest;
import com.sistemadequejas.dto.RecuperarSolicitarRequest;
import com.sistemadequejas.dto.RegistroRequest;
import com.sistemadequejas.model.RecuperacionContrasena;
import com.sistemadequejas.model.Usuario;
import com.sistemadequejas.service.BitacoraAuditoriaService;
import com.sistemadequejas.service.EmailService;
import com.sistemadequejas.service.RecuperacionContrasenaService;
import com.sistemadequejas.service.UsuarioService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String SESSION_ID_USUARIO = "idUsuario";

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private RecuperacionContrasenaService recuperacionContrasenaService;

    @Autowired
    private BitacoraAuditoriaService bitacoraAuditoriaService;

    @Autowired
    private EmailService emailService;

    // CU-01, FA01.1: permite validar el correo antes de pedir la contrasena.
    @GetMapping("/correo-disponible")
    public ResponseEntity<Map<String, Boolean>> correoDisponible(@RequestParam String correo) {
        boolean disponible = usuarioService.buscarPorCorreo(correo).isEmpty();
        return ResponseEntity.ok(Map.of("disponible", disponible));
    }

    // CU-01: crea la cuenta de cliente.
    @PostMapping("/registro")
    public ResponseEntity<Usuario> registro(@RequestBody RegistroRequest request) {
        Usuario usuario = new Usuario();
        usuario.setNombreCompleto(request.getNombreCompleto());
        usuario.setFechaNacimiento(request.getFechaNacimiento());
        usuario.setNacionalidad(request.getNacionalidad());
        usuario.setCorreo(request.getCorreo());
        usuario.setCodigoArea(request.getCodigoArea());
        usuario.setTelefono(request.getTelefono());
        usuario.setDireccion(request.getDireccion());

        Usuario creado = usuarioService.registrar(usuario, request.getPassword());
        creado.setContrasenaHash(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    // CU-00: valida credenciales y abre sesion del lado del servidor.
    @PostMapping("/login")
    public ResponseEntity<Usuario> login(@RequestBody LoginRequest request, HttpSession session) {
        Usuario usuario = usuarioService.autenticar(request.getCorreo(), request.getPassword());
        session.setAttribute(SESSION_ID_USUARIO, usuario.getIdUsuario());
        usuario.setContrasenaHash(null);
        return ResponseEntity.ok(usuario);
    }

    // CU-04: destruye la sesion activa.
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.noContent().build();
    }

    // Usado por el frontend para saber si hay sesion activa y cargar el perfil (CU-03).
    @GetMapping("/me")
    public ResponseEntity<Usuario> me(HttpSession session) {
        Usuario usuario = usuarioAutenticado(session);
        usuario.setContrasenaHash(null);
        return ResponseEntity.ok(usuario);
    }

    // CU-03: actualiza datos personales y, si se envia, la contrasena.
    @PutMapping("/perfil")
    public ResponseEntity<Usuario> actualizarPerfil(@RequestBody ActualizarPerfilRequest request, HttpSession session) {
        Usuario usuario = usuarioAutenticado(session);

        // FA02: el correo nuevo no debe pertenecer a otra cuenta (se valida ANTES de mutar la entidad).
        usuarioService.validarCorreoDisponibleParaOtroUsuario(request.getCorreo(), usuario.getIdUsuario());

        usuario.setNombreCompleto(request.getNombreCompleto());
        usuario.setFechaNacimiento(request.getFechaNacimiento());
        usuario.setNacionalidad(request.getNacionalidad());
        usuario.setCorreo(request.getCorreo());
        usuario.setCodigoArea(request.getCodigoArea());
        usuario.setTelefono(request.getTelefono());
        usuario.setDireccion(request.getDireccion());

        Usuario actualizado = usuarioService.actualizarPerfil(usuario, request.getPasswordActual(), request.getPasswordNueva());
        actualizado.setContrasenaHash(null);

        // CU-15: deja constancia del cambio en la bitacora de auditoria.
        bitacoraAuditoriaService.registrarAccionUsuario(actualizado, "Actualizacion de perfil", "CU-03", "El usuario actualizo sus datos personales");

        return ResponseEntity.ok(actualizado);
    }

    // CU-02 paso 1: genera el codigo de recuperacion y lo envia por correo.
    @PostMapping("/recuperar/solicitar")
    public ResponseEntity<Map<String, String>> recuperarSolicitar(@RequestBody RecuperarSolicitarRequest request) {
        Optional<Usuario> usuarioOpt = usuarioService.buscarPorCorreo(request.getCorreo());
        // FA03: mensaje de seguridad neutro, igual exista o no la cuenta.
        String mensaje = "Se enviaran instrucciones de recuperacion si el correo electronico es valido";

        if (usuarioOpt.isEmpty()) {
            return ResponseEntity.ok(Map.of("mensaje", mensaje));
        }

        RecuperacionContrasena recuperacion = recuperacionContrasenaService.generarCodigo(usuarioOpt.get());

        emailService.enviar(
                request.getCorreo(),
                "Recuperacion de contrasena - Sistema de Quejas Las Delicias",
                "Tu codigo de recuperacion es: " + recuperacion.getCodigoToken()
                        + "\n\nEste codigo vence en 15 minutos. Si no solicitaste este cambio, ignora este mensaje."
        );

        return ResponseEntity.ok(Map.of("mensaje", mensaje));
    }

    // CU-02 paso 2: valida el codigo y aplica la nueva contrasena.
    @PostMapping("/recuperar/confirmar")
    public ResponseEntity<Void> recuperarConfirmar(@RequestBody RecuperarConfirmarRequest request) {
        Usuario usuario = usuarioService.buscarPorCorreo(request.getCorreo())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "El codigo de recuperacion ingresado es incorrecto"));

        recuperacionContrasenaService.validarYConsumirCodigo(usuario, request.getCodigo());
        usuarioService.restablecerPassword(usuario, request.getPasswordNueva());
        return ResponseEntity.noContent().build();
    }

    private Usuario usuarioAutenticado(HttpSession session) {
        Object idUsuario = session.getAttribute(SESSION_ID_USUARIO);
        if (idUsuario == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Debe iniciar sesion");
        }
        return usuarioService.findById((Integer) idUsuario)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Debe iniciar sesion"));
    }
}
