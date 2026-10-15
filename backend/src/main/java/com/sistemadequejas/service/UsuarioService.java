package com.sistemadequejas.service;

import com.sistemadequejas.model.Usuario;
import com.sistemadequejas.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class UsuarioService {

  private static final Pattern PATRON_PASSWORD = Pattern.compile("^(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*(),.?\":{}|<>_\\-]).+$");
  private static final Pattern PATRON_CORREO = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
  private static final Pattern PATRON_SOLO_LETRAS = Pattern.compile("^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ ]+$");
  private static final Pattern PATRON_NUMERICO = Pattern.compile("^[0-9]+$");

  public static final String MSG_LONGITUD_PASSWORD = "La contraseña debe tener entre 6 y 20 caracteres";
  public static final String MSG_FORMATO_PASSWORD_CU01 =
    "El formato de la contraseña debe incluir al menos una letra mayúscula, un carácter especial y un número";
  public static final String MSG_FORMATO_PASSWORD_CU02 =
    "El formato de la contraseña debe incluir al menos una letra mayúscula, un número y un carácter especial";

  @Autowired
  private UsuarioRepository usuarioRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  private void validarFormatoPassword(String passwordPlano, String mensajeFormato) {
    if (passwordPlano == null || passwordPlano.length() < 6 || passwordPlano.length() > 20) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, MSG_LONGITUD_PASSWORD);
    }
    if (!PATRON_PASSWORD.matcher(passwordPlano).matches()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, mensajeFormato);
    }
  }

  // CU-01: código de área numérico de hasta 4 caracteres; teléfono de 8 dígitos
  private String primerCampoInvalido(Usuario u) {
    if (u.getNombreCompleto() == null || u.getNombreCompleto().isBlank() || u.getNombreCompleto().length() > 100
      || !PATRON_SOLO_LETRAS.matcher(u.getNombreCompleto()).matches()) {
      return "nombre completo";
    }
    if (u.getFechaNacimiento() == null || u.getFechaNacimiento().isAfter(LocalDate.now())) {
      return "fecha de nacimiento";
    }
    if (u.getNacionalidad() == null || u.getNacionalidad().isBlank() || u.getNacionalidad().length() > 50) {
      return "nacionalidad";
    }
    if (u.getCorreo() == null || u.getCorreo().length() > 100 || !PATRON_CORREO.matcher(u.getCorreo().trim()).matches()) {
      return "correo electrónico";
    }
    // CU-01 permite hasta 4 caracteres numéricos (ej. 502)
    if (u.getCodigoArea() == null || u.getCodigoArea().isBlank() || u.getCodigoArea().length() > 4 || !PATRON_NUMERICO.matcher(u.getCodigoArea()).matches()) {
      return "código de área telefónico";
    }
    if (u.getTelefono() == null || u.getTelefono().length() != 8 || !PATRON_NUMERICO.matcher(u.getTelefono()).matches()) {
      return "número de teléfono";
    }
    if (u.getDireccion() == null || u.getDireccion().isBlank() || u.getDireccion().length() > 150) {
      return "dirección";
    }
    return null;
  }

  public List<Usuario> findAll() {
    return usuarioRepository.findAll();
  }

  public Optional<Usuario> findById(Integer id) {
    return usuarioRepository.findById(id);
  }

  public Optional<Usuario> buscarPorCorreo(String correo) {
    return usuarioRepository.findByCorreo(correo);
  }

  @Transactional
  public Usuario save(Usuario usuario) {
    return usuarioRepository.save(usuario);
  }

  @Transactional
  public void deleteById(Integer id) {
    usuarioRepository.deleteById(id);
  }

  // CU-01 Flujo 1: Registrarse
  @Transactional
  public Usuario registrar(Usuario usuario, String passwordPlano) {
    String campoError = primerCampoInvalido(usuario);
    if (campoError != null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
        "Debe completar todos los datos personales obligatorios antes de continuar");
    }
    if (usuarioRepository.existsByCorreo(usuario.getCorreo())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo electrónico ya se encuentra registrado");
    }
    validarFormatoPassword(passwordPlano, MSG_FORMATO_PASSWORD_CU01);
    usuario.setContrasenaHash(passwordEncoder.encode(passwordPlano));
    return usuarioRepository.saveAndFlush(usuario);
  }

  // CU-00: Login
  public Usuario autenticar(String correo, String passwordPlano) {
    Usuario usuario = usuarioRepository.findByCorreo(correo == null ? "" : correo.trim())
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Correo electrónico o contraseña incorrectos"));

    if (passwordPlano == null || !passwordEncoder.matches(passwordPlano, usuario.getContrasenaHash())) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Correo electrónico o contraseña incorrectos");
    }
    return usuario;
  }

  public void validarCorreoDisponibleParaOtroUsuario(String correoNuevo, Integer idUsuarioActual) {
    usuarioRepository.findByCorreo(correoNuevo).ifPresent(existente -> {
      if (!existente.getIdUsuario().equals(idUsuarioActual)) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo electrónico ya se encuentra registrado");
      }
    });
  }

  public void validarDatosPerfil(Usuario datosNuevos) {
    String campo = primerCampoInvalido(datosNuevos);
    if (campo != null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Formato de datos no válido. Revise el campo: " + campo);
    }
  }

  // CU-01 Flujo 3: Actualizar perfil
  @Transactional
  public Usuario actualizarPerfil(Usuario usuario, String passwordActualPlano, String passwordNuevaPlano) {
    String campoError = primerCampoInvalido(usuario);
    if (campoError != null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Formato de datos no válido. Revise el campo: " + campoError);
    }

    if (passwordNuevaPlano != null && !passwordNuevaPlano.isBlank()) {
      if (passwordActualPlano == null || !passwordEncoder.matches(passwordActualPlano, usuario.getContrasenaHash())) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Contraseña actual incorrecta");
      }
      validarFormatoPassword(passwordNuevaPlano, MSG_FORMATO_PASSWORD_CU02);
      usuario.setContrasenaHash(passwordEncoder.encode(passwordNuevaPlano));
    }
    return usuarioRepository.saveAndFlush(usuario);
  }

  // CU-01 Flujo 2: Recuperación de contraseña
  @Transactional
  public void restablecerPassword(Usuario usuario, String passwordNuevaPlano) {
    validarFormatoPassword(passwordNuevaPlano, MSG_FORMATO_PASSWORD_CU02);
    usuario.setContrasenaHash(passwordEncoder.encode(passwordNuevaPlano));
    usuarioRepository.saveAndFlush(usuario);
  }
}
