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

  @GetMapping("/correo-disponible")
  public ResponseEntity<Map<String, Boolean>> correoDisponible(@RequestParam String correo) {
    boolean disponible = usuarioService.buscarPorCorreo(correo).isEmpty()
      && personalService.findByCorreo(correo).isEmpty();
    return ResponseEntity.ok(Map.of("disponible", disponible));
  }

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

    try {
      bitacoraAuditoriaService.registrarAccionUsuario(
        creado,
        "Registro de cuenta",
        "CU-01",
        "Creación de nueva cuenta de cliente para: " + creado.getCorreo()
      );
    } catch (Exception e) {
      e.printStackTrace();
    }

    creado.setContrasenaHash(null);
    return ResponseEntity.status(HttpStatus.CREATED).body(creado);
  }

  @PostMapping("/login")
  public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpSession session) {
    String correo = request.getCorreo();
    String password = request.getPassword();

    if (correo == null || correo.trim().isEmpty() || password == null || password.trim().isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe ingresar los campos obligatorios");
    }

    correo = correo.trim();

    if (correo.length() > 100 || !PATRON_EMAIL.matcher(correo).matches()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El formato del correo electrónico es inválido");
    }

    if (password.length() > 20) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña no debe superar los 20 caracteres");
    }

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
    }

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
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Correo electrónico o contraseña incorrectos");
    }
  }

  @PostMapping("/logout")
  public ResponseEntity<Map<String, String>> logout(HttpSession session) {
    if (session != null) {
      try {
        session.invalidate();
      } catch (IllegalStateException e) {
      }
    }
    return ResponseEntity.ok(Map.of("message", "Sesión finalizada correctamente"));
  }

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

  @PutMapping("/perfil")
  public ResponseEntity<Usuario> actualizarPerfil(@RequestBody ActualizarPerfilRequest request, HttpSession session) {
    Usuario usuario = usuarioAutenticado(session);

    usuarioService.validarCorreoDisponibleParaOtroUsuario(request.getCorreo(), usuario.getIdUsuario());

    String anteriorJson = BitacoraAuditoriaService.json(
      "nombre", usuario.getNombreCompleto(),
      "fechaNacimiento", usuario.getFechaNacimiento() != null ? usuario.getFechaNacimiento().toString() : "",
      "nacionalidad", usuario.getNacionalidad(),
      "correo", usuario.getCorreo(),
      "codigoArea", usuario.getCodigoArea(),
      "telefono", usuario.getTelefono(),
      "direccion", usuario.getDireccion()
    );

    usuario.setNombreCompleto(request.getNombreCompleto());
    usuario.setFechaNacimiento(request.getFechaNacimiento());
    usuario.setNacionalidad(request.getNacionalidad());
    usuario.setCorreo(request.getCorreo());
    usuario.setCodigoArea(request.getCodigoArea());
    usuario.setTelefono(request.getTelefono());
    usuario.setDireccion(request.getDireccion());

    Usuario actualizado = usuarioService.actualizarPerfil(usuario, request.getPasswordActual(), request.getPasswordNueva());

    boolean cambioPass = request.getPasswordNueva() != null && !request.getPasswordNueva().isBlank();
    String nuevoJson = BitacoraAuditoriaService.json(
      "nombre", actualizado.getNombreCompleto(),
      "fechaNacimiento", actualizado.getFechaNacimiento() != null ? actualizado.getFechaNacimiento().toString() : "",
      "nacionalidad", actualizado.getNacionalidad(),
      "correo", actualizado.getCorreo(),
      "codigoArea", actualizado.getCodigoArea(),
      "telefono", actualizado.getTelefono(),
      "direccion", actualizado.getDireccion(),
      "cambioContrasena", String.valueOf(cambioPass)
    );

    try {
      bitacoraAuditoriaService.registrarAccionUsuario(
        actualizado,
        "Actualización de perfil",
        "CU-01",
        BitacoraAuditoriaService.cambio(anteriorJson, nuevoJson)
      );
    } catch (Exception e) {
      e.printStackTrace();
    }

    actualizado.setContrasenaHash(null);
    return ResponseEntity.ok(actualizado);
  }

  @PostMapping("/recuperar/solicitar")
  public ResponseEntity<Map<String, String>> recuperarSolicitar(@RequestBody RecuperarSolicitarRequest request) {
    Usuario usuario = usuarioService.buscarPorCorreo(request.getCorreo())
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Correo no registrado"));

    RecuperacionContrasena recuperacion = recuperacionContrasenaService.generarCodigo(usuario);

    emailService.enviar(
      request.getCorreo(),
      "Recuperación de contraseña - Sistema de Quejas Las Delicias",
      "Tu código de recuperación es: " + recuperacion.getCodigoToken()
        + "\n\nEste código vence en 15 minutos. Si no solicitaste este cambio, ignora este mensaje."
    );

    return ResponseEntity.ok(Map.of("mensaje", "Se enviaron instrucciones de recuperación a tu correo"));
  }

  @PostMapping("/recuperar/confirmar")
  public ResponseEntity<Void> recuperarConfirmar(@RequestBody RecuperarConfirmarRequest request) {
    Usuario usuario = usuarioService.buscarPorCorreo(request.getCorreo())
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "El código de recuperación ingresado es incorrecto"));

    RecuperacionContrasena recuperacion = recuperacionContrasenaService.validarCodigo(usuario, request.getCodigo());
    usuarioService.restablecerPassword(usuario, request.getPasswordNueva());
    recuperacionContrasenaService.consumir(recuperacion);

    try {
      bitacoraAuditoriaService.registrarAccionUsuario(
        usuario,
        "Recuperación de contraseña",
        "CU-01",
        "Restablecimiento exitoso de contraseña mediante código de verificación para: " + usuario.getCorreo()
      );
    } catch (Exception e) {
      e.printStackTrace();
    }

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
