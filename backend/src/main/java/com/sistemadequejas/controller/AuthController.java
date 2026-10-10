package com.sistemadequejas.controller;

import com.sistemadequejas.dto.*;
import com.sistemadequejas.model.Personal;
import com.sistemadequejas.model.RecuperacionContrasena;
import com.sistemadequejas.model.Usuario;
import com.sistemadequejas.service.*;
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
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
public class AuthController {

  private static final String SESSION_ID_USUARIO = "idUsuario";
  private static final String SESSION_TIPO = "tipoUsuario";
  private static final String SESSION_ROL = "rolUsuario";

  // Formato estandar para email (CU-00 Flujo 1)
  private static final Pattern PATRON_EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

  @Autowired
  private UsuarioService usuarioService;

  @Autowired
  private PersonalService personalService;

  @Autowired
  private RecuperacionContrasenaService recuperacionContrasenaService;

  @Autowired
  private BitacoraAuditoriaService bitacoraAuditoriaService;

  @Autowired
  private EmailService emailService;

  // CU-01, FA01.1: valida disponibilidad de correo
  @GetMapping("/correo-disponible")
  public ResponseEntity<Map<String, Boolean>> correoDisponible(@RequestParam String correo) {
    boolean disponible = usuarioService.buscarPorCorreo(correo).isEmpty()
      && personalService.findByCorreo(correo).isEmpty();
    return ResponseEntity.ok(Map.of("disponible", disponible));
  }

  // CU-01: registro de cliente
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

  // CU-00: Flujo normal 1 y flujos alternos FA04, FA05
  @PostMapping("/login")
  public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpSession session) {
    String correo = request.getCorreo();
    String password = request.getPassword();

    // FA04: Campos obligatorios vacios
    if (correo == null || correo.trim().isEmpty() || password == null || password.trim().isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe ingresar los campos obligatorios");
    }

    correo = correo.trim();

    // Validacion de longitud y formato email segun especificacion CU-00 (max 100 caracteres)
    if (correo.length() > 100 || !PATRON_EMAIL.matcher(correo).matches()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El formato del correo electrónico es inválido");
    }

    // Validacion de longitud de contrasena (max 20 caracteres)
    if (password.length() > 20) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña no debe superar los 20 caracteres");
    }

    // 1. Intentar autenticar como Cliente
    try {
      Usuario usuario = usuarioService.autenticar(correo, password);
      session.setAttribute(SESSION_ID_USUARIO, usuario.getIdUsuario());
      session.setAttribute(SESSION_TIPO, "CLIENTE");
      session.setAttribute(SESSION_ROL, "CLIENTE");

      LoginResponse response = new LoginResponse(
        usuario.getIdUsuario(),
        usuario.getNombreCompleto(),
        usuario.getCorreo(),
        "CLIENTE",
        "CLIENTE"
      );
      return ResponseEntity.ok(response);
    } catch (ResponseStatusException exCliente) {
      // Si el cliente no existe o sus credenciales fallaron, intentamos con Personal interno
    }

    // 2. Intentar autenticar como Personal interno (Administrador General, Gerente, Operador)
    try {
      Personal personal = personalService.autenticar(correo, password);
      String nombreRol = personal.getRol() != null ? personal.getRol().getNombre() : "OPERADOR";

      String rolCodigo = "OPERADOR";
      if (nombreRol.toUpperCase().contains("ADMIN")) {
        rolCodigo = "ADMINISTRADOR";
      } else if (nombreRol.toUpperCase().contains("GERENTE")) {
        rolCodigo = "GERENTE";
      }

      session.setAttribute(SESSION_ID_USUARIO, personal.getIdPersonal());
      session.setAttribute(SESSION_TIPO, "PERSONAL");
      session.setAttribute(SESSION_ROL, rolCodigo);

      LoginResponse response = new LoginResponse(
        personal.getIdPersonal(),
        personal.getNombreCompleto(),
        personal.getCorreo(),
        rolCodigo,
        "PERSONAL"
      );
      return ResponseEntity.ok(response);
    } catch (ResponseStatusException exPersonal) {
      // FA05: Credenciales incorrectas para ambos
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Correo electrónico o contraseña incorrectos");
    }
  }

  // CU-00: Flujo normal 2 y FA06 (Cerrar sesion)
  @PostMapping("/logout")
  public ResponseEntity<Map<String, String>> logout(HttpSession session) {
    if (session != null) {
      try {
        session.invalidate();
      } catch (IllegalStateException e) {
        // FA06: Sesion ya expirada, se continua sin error
      }
    }
    return ResponseEntity.ok(Map.of("message", "Sesión finalizada correctamente"));
  }

  // CU-00 / CU-03: Informacion del usuario autenticado
  @GetMapping("/me")
  public ResponseEntity<?> me(HttpSession session) {
    Object idUsuario = session.getAttribute(SESSION_ID_USUARIO);
    Object tipo = session.getAttribute(SESSION_TIPO);

    if (idUsuario == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Debe iniciar sesion");
    }

    if ("PERSONAL".equals(tipo)) {
      Personal p = personalService.findById((Integer) idUsuario)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Debe iniciar sesion"));
      p.setContrasenaHash(null);
      return ResponseEntity.ok(p);
    } else {
      Usuario u = usuarioService.findById((Integer) idUsuario)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Debe iniciar sesion"));
      u.setContrasenaHash(null);
      return ResponseEntity.ok(u);
    }
  }

  // CU-03: Actualizar perfil de cliente
  @PutMapping("/perfil")
  public ResponseEntity<Usuario> actualizarPerfil(@RequestBody ActualizarPerfilRequest request, HttpSession session) {
    Usuario usuario = usuarioAutenticado(session);

    usuarioService.validarCorreoDisponibleParaOtroUsuario(request.getCorreo(), usuario.getIdUsuario());

    usuario.setNombreCompleto(request.getNombreCompleto());
    usuario.setFechaNacimiento(request.getFechaNacimiento());
    usuario.setNacionalidad(request.getNacionalidad());
    usuario.setCorreo(request.getCorreo());
    usuario.setCodigoArea(request.getCodigoArea());
    usuario.setTelefono(request.getTelefono());
    usuario.setDireccion(request.getDireccion());

    Usuario actualizado = usuarioService.actualizarPerfil(usuario, request.getPasswordActual(), request.getPasswordNueva());

    try {
      bitacoraAuditoriaService.registrarAccionUsuario(actualizado, "Actualizacion de perfil", "CU-03", "El usuario actualizo sus datos personales");
    } catch (Exception e) {
      System.err.println("Advertencia al auditar CU-03: " + e.getMessage());
    }

    actualizado.setContrasenaHash(null);
    return ResponseEntity.ok(actualizado);
  }

  // CU-02 paso 1
  @PostMapping("/recuperar/solicitar")
  public ResponseEntity<Map<String, String>> recuperarSolicitar(@RequestBody RecuperarSolicitarRequest request) {
    Usuario usuario = usuarioService.buscarPorCorreo(request.getCorreo())
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Correo no registrado"));

    RecuperacionContrasena recuperacion = recuperacionContrasenaService.generarCodigo(usuario);

    emailService.enviar(
      request.getCorreo(),
      "Recuperacion de contrasena - Sistema de Quejas Las Delicias",
      "Tu codigo de recuperacion es: " + recuperacion.getCodigoToken()
        + "\n\nEste codigo vence en 15 minutos. Si no solicitaste este cambio, ignora este mensaje."
    );

    return ResponseEntity.ok(Map.of("mensaje", "Se enviaron instrucciones de recuperacion a tu correo"));
  }

  // CU-02 paso 2: valida el codigo y aplica la nueva contrasena.
  @PostMapping("/recuperar/confirmar")
  public ResponseEntity<Void> recuperarConfirmar(@RequestBody RecuperarConfirmarRequest request) {
    Usuario usuario = usuarioService.buscarPorCorreo(request.getCorreo())
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "El código de recuperación ingresado es incorrecto"));

    RecuperacionContrasena recuperacion = recuperacionContrasenaService.validarCodigo(usuario, request.getCodigo());
    usuarioService.restablecerPassword(usuario, request.getPasswordNueva());
    recuperacionContrasenaService.consumir(recuperacion);
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
