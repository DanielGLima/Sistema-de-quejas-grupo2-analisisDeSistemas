package com.sistemadequejas.controller;

import com.sistemadequejas.dto.ActualizarPerfilRequest;
import com.sistemadequejas.dto.LoginRequest;
import com.sistemadequejas.dto.LoginResponse;
import com.sistemadequejas.dto.RecuperarConfirmarRequest;
import com.sistemadequejas.dto.RecuperarSolicitarRequest;
import com.sistemadequejas.dto.RegistroRequest;
import com.sistemadequejas.config.SesionesActivas;
import com.sistemadequejas.config.SesionPersonal;
import com.sistemadequejas.model.Personal;
import com.sistemadequejas.model.RecuperacionContrasena;
import com.sistemadequejas.model.Usuario;
import com.sistemadequejas.service.BitacoraAuditoriaService;
import com.sistemadequejas.service.EmailService;
import com.sistemadequejas.service.PersonalService;
import com.sistemadequejas.service.RecuperacionContrasenaService;
import com.sistemadequejas.service.UsuarioService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String SESSION_ID_USUARIO = "idUsuario";
    private static final String SESSION_TIPO = "tipoUsuario";
    private static final String SESSION_ROL = "rolUsuario";

    // Formato estandar de correo electronico (CU-00).
    private static final Pattern PATRON_EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

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

    @Autowired
    private PersonalService personalService;

    // CU-01, FA01.1: permite validar el correo antes de pedir la contrasena.
    @GetMapping("/correo-disponible")
    public ResponseEntity<Map<String, Boolean>> correoDisponible(@RequestParam String correo) {
        boolean disponible = usuarioService.buscarPorCorreo(correo).isEmpty()
                && personalService.findByCorreo(correo).isEmpty();
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

    // CU-00: una sola pantalla de acceso para clientes y personal interno.
    // Primero se intenta como cliente y, si no corresponde, como personal; las credenciales
    // incorrectas devuelven siempre el mismo mensaje (no se revela cual dato fallo).
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request, HttpSession session) {
        String correo = request.getCorreo() == null ? "" : request.getCorreo().trim();
        String password = request.getPassword() == null ? "" : request.getPassword();

        // FA04: campos obligatorios vacios.
        if (correo.isEmpty() || password.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe ingresar los campos obligatorios");
        }
        if (correo.length() > 100 || !PATRON_EMAIL.matcher(correo).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El formato del correo electrónico es inválido");
        }
        if (password.length() > 20) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña no debe superar los 20 caracteres");
        }

        // 1. Cliente
        try {
            Usuario usuario = usuarioService.autenticar(correo, password);
            session.setAttribute(SESSION_ID_USUARIO, usuario.getIdUsuario());
            session.setAttribute(SESSION_TIPO, "CLIENTE");
            session.setAttribute(SESSION_ROL, "CLIENTE");
            sesionesActivas.registrar(usuario.getIdUsuario(), session);
            return ResponseEntity.ok(new LoginResponse(
                    usuario.getIdUsuario(), usuario.getNombreCompleto(), usuario.getCorreo(), "CLIENTE", "CLIENTE"));
        } catch (ResponseStatusException noEsCliente) {
            // Se intenta como personal interno.
        }

        // 2. Personal interno (Administrador General, Gerente u Operador)
        try {
            Personal personal = personalService.autenticar(correo, password);
            String nombreRol = personal.getRol() == null ? "" : personal.getRol().getNombre().toUpperCase();
            String rolCodigo = nombreRol.contains("ADMIN") ? "ADMINISTRADOR" : nombreRol.contains("GERENTE") ? "GERENTE" : "OPERADOR";

            // La sesion de personal usa su propio atributo (lo leen CU-07 a CU-10).
            session.setAttribute(SesionPersonal.SESSION_ID_PERSONAL, personal.getIdPersonal());
            session.setAttribute(SESSION_TIPO, "PERSONAL");
            session.setAttribute(SESSION_ROL, rolCodigo);
            bitacoraAuditoriaService.registrarAccionPersonal(personal, null, "Inicio de sesión", "Acceso al portal",
                    BitacoraAuditoriaService.cambio("{}", BitacoraAuditoriaService.json("rol", personal.getRol().getNombre())));
            return ResponseEntity.ok(new LoginResponse(
                    personal.getIdPersonal(), personal.getNombreCompleto(), personal.getCorreo(), rolCodigo, "PERSONAL"));
        } catch (ResponseStatusException noEsPersonal) {
            // FA05: credenciales incorrectas.
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Correo electrónico o contraseña incorrectos");
        }
    }

    // CU-04: destruye la sesion activa.
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.noContent().build();
    }

    // Usado por el frontend para saber si hay sesion activa y cargar el perfil (CU-03).
    @GetMapping("/me")
    public ResponseEntity<?> me(HttpSession session) {
        Object idPersonal = session.getAttribute(SesionPersonal.SESSION_ID_PERSONAL);
        if (idPersonal != null) {
            return ResponseEntity.ok(personalService.findById((Integer) idPersonal)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Debe iniciar sesión")));
        }
        return ResponseEntity.ok(usuarioAutenticado(session));
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
