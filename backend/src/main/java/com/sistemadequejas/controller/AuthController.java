package com.sistemadequejas.controller;

import com.sistemadequejas.dto.ActualizarPerfilRequest;
import com.sistemadequejas.dto.LoginRequest;
import com.sistemadequejas.dto.RecuperarConfirmarRequest;
import com.sistemadequejas.dto.RecuperarSolicitarRequest;
import com.sistemadequejas.dto.RegistroRequest;
import com.sistemadequejas.config.SesionesActivas;
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

    @Autowired
    private SesionesActivas sesionesActivas;

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
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    // CU-00: valida credenciales y abre sesion del lado del servidor.
    @PostMapping("/login")
    public ResponseEntity<Usuario> login(@RequestBody LoginRequest request, HttpSession session) {
        Usuario usuario = usuarioService.autenticar(request.getCorreo(), request.getPassword());
        session.setAttribute(SESSION_ID_USUARIO, usuario.getIdUsuario());
        sesionesActivas.registrar(usuario.getIdUsuario(), session);
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
        return ResponseEntity.ok(usuario);
    }

    // CU-03: actualiza datos personales y, si se envia, la contrasena.
    @PutMapping("/perfil")
    public ResponseEntity<Usuario> actualizarPerfil(@RequestBody ActualizarPerfilRequest request, HttpSession session) {
        Usuario usuario = usuarioAutenticado(session);

        Usuario datosNuevos = new Usuario();
        datosNuevos.setNombreCompleto(request.getNombreCompleto());
        datosNuevos.setFechaNacimiento(request.getFechaNacimiento());
        datosNuevos.setNacionalidad(request.getNacionalidad());
        datosNuevos.setCorreo(request.getCorreo());
        datosNuevos.setCodigoArea(request.getCodigoArea());
        datosNuevos.setTelefono(request.getTelefono());
        datosNuevos.setDireccion(request.getDireccion());

        // Paso 5 / FA03: formato de datos no valido (se indica el campo con error).
        usuarioService.validarDatosPerfil(datosNuevos);
        // FA02: el correo nuevo no debe pertenecer a otra cuenta (se valida ANTES de mutar la entidad).
        usuarioService.validarCorreoDisponibleParaOtroUsuario(request.getCorreo(), usuario.getIdUsuario());

        String anterior = BitacoraAuditoriaService.json(
                "nombreCompleto", usuario.getNombreCompleto(),
                "fechaNacimiento", String.valueOf(usuario.getFechaNacimiento()),
                "nacionalidad", usuario.getNacionalidad(),
                "correo", usuario.getCorreo(),
                "codigoArea", usuario.getCodigoArea(),
                "telefono", usuario.getTelefono(),
                "direccion", usuario.getDireccion());

        // FA01: si cambia la contrasena se valida la actual ANTES de aplicar cualquier cambio.
        boolean cambiaPassword = request.getPasswordNueva() != null && !request.getPasswordNueva().isBlank();

        usuario.setNombreCompleto(datosNuevos.getNombreCompleto());
        usuario.setFechaNacimiento(datosNuevos.getFechaNacimiento());
        usuario.setNacionalidad(datosNuevos.getNacionalidad());
        usuario.setCorreo(datosNuevos.getCorreo());
        usuario.setCodigoArea(datosNuevos.getCodigoArea());
        usuario.setTelefono(datosNuevos.getTelefono());
        usuario.setDireccion(datosNuevos.getDireccion());

        Usuario actualizado = usuarioService.actualizarPerfil(usuario, request.getPasswordActual(), request.getPasswordNueva());

        // Paso 7: registro en la bitacora de auditoria (CU-15) con valores anteriores y nuevos.
        String nuevo = BitacoraAuditoriaService.json(
                "nombreCompleto", actualizado.getNombreCompleto(),
                "fechaNacimiento", String.valueOf(actualizado.getFechaNacimiento()),
                "nacionalidad", actualizado.getNacionalidad(),
                "correo", actualizado.getCorreo(),
                "codigoArea", actualizado.getCodigoArea(),
                "telefono", actualizado.getTelefono(),
                "direccion", actualizado.getDireccion(),
                "contraseñaModificada", cambiaPassword ? "Sí" : "No");
        bitacoraAuditoriaService.registrarAccionUsuario(actualizado, "Actualización de perfil", "Perfil de usuario",
                BitacoraAuditoriaService.cambio(anterior, nuevo));

        return ResponseEntity.ok(actualizado);
    }

    // CU-02 paso 1: genera el codigo de recuperacion y lo envia por correo.
    // NOTA: por decision del equipo se revela si el correo esta registrado ("Correo no registrado"),
    // en lugar del mensaje neutro del FA03 original; el documento CU-02 se actualizo en ese sentido.
    @PostMapping("/recuperar/solicitar")
    public ResponseEntity<Map<String, String>> recuperarSolicitar(@RequestBody RecuperarSolicitarRequest request) {
        Usuario usuario = usuarioService.buscarPorCorreo(request.getCorreo())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Correo no registrado"));

        RecuperacionContrasena recuperacion = recuperacionContrasenaService.generarCodigo(usuario);

        boolean enviado = emailService.enviar(
                request.getCorreo(),
                "Recuperación de contraseña - Sistema de Quejas Las Delicias",
                "Tu código de recuperación es: " + recuperacion.getCodigoToken()
                        + "\n\nEste código vence en 15 minutos. Si no solicitaste este cambio, ignora este mensaje."
        );
        if (!enviado) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudo enviar el correo con el código de recuperación. Intente nuevamente en unos minutos");
        }

        return ResponseEntity.ok(Map.of("mensaje", "Se enviaron instrucciones de recuperación a tu correo"));
    }

    // CU-02 pasos 6 a 9: valida el codigo y aplica la nueva contrasena.
    @PostMapping("/recuperar/confirmar")
    public ResponseEntity<Void> recuperarConfirmar(@RequestBody RecuperarConfirmarRequest request) {
        Usuario usuario = usuarioService.buscarPorCorreo(request.getCorreo())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "El código de recuperación ingresado es incorrecto"));

        // FA05 / FA06: valida el codigo SIN consumirlo; FA08 / FA09 devuelven al paso 7 con el mismo codigo vigente.
        RecuperacionContrasena recuperacion = recuperacionContrasenaService.validarCodigo(usuario, request.getCodigo());
        usuarioService.restablecerPassword(usuario, request.getPasswordNueva());
        recuperacionContrasenaService.consumir(recuperacion);

        // Postcondicion: las sesiones activas previas quedan invalidadas.
        sesionesActivas.invalidarTodas(usuario.getIdUsuario());

        bitacoraAuditoriaService.registrarAccionUsuario(usuario, "Restablecimiento de contraseña", "Recuperación de contraseña",
                BitacoraAuditoriaService.cambio("{}", BitacoraAuditoriaService.json("contraseñaRestablecida", "Sí")));
        return ResponseEntity.noContent().build();
    }

    private Usuario usuarioAutenticado(HttpSession session) {
        Object idUsuario = session.getAttribute(SESSION_ID_USUARIO);
        if (idUsuario == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Debe iniciar sesión");
        }
        return usuarioService.findById((Integer) idUsuario)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Debe iniciar sesión"));
    }
}
