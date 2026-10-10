package com.sistemadequejas.service;

import com.sistemadequejas.model.Usuario;
import com.sistemadequejas.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class UsuarioService {

    // Al menos una mayuscula, un numero y un caracter especial (CU-01 FA02, CU-02 FA09).
    private static final Pattern PATRON_PASSWORD = Pattern.compile("^(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*(),.?\":{}|<>_\\-]).+$");
    private static final Pattern PATRON_CORREO = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern PATRON_SOLO_LETRAS = Pattern.compile("^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ ]+$");
    private static final Pattern PATRON_NUMERICO = Pattern.compile("^[0-9]+$");

    public static final String MSG_LONGITUD_PASSWORD = "La contraseña debe tener entre 6 y 20 caracteres";
    // CU-01 FA02 usa este orden de palabras...
    public static final String MSG_FORMATO_PASSWORD_CU01 =
            "El formato de la contraseña debe incluir al menos una letra mayúscula, un carácter especial y un número";
    // ...y CU-02 FA09 / CU-03 usan este otro.
    public static final String MSG_FORMATO_PASSWORD_CU02 =
            "El formato de la contraseña debe incluir al menos una letra mayúscula, un número y un carácter especial";

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Valida longitud (6 a 20) y formato de una contrasena nueva; el texto del formato depende del caso de uso.
    private void validarFormatoPassword(String passwordPlano, String mensajeFormato) {
        if (passwordPlano == null || passwordPlano.length() < 6 || passwordPlano.length() > 20) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, MSG_LONGITUD_PASSWORD);
        }
        if (!PATRON_PASSWORD.matcher(passwordPlano).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, mensajeFormato);
        }
    }

  // CU-01 FA01 / CU-03 FA03: valida los datos personales; devuelve el nombre del primer campo inválido o null.
  private String primerCampoInvalido(Usuario u) {
    if (u.getNombreCompleto() == null || u.getNombreCompleto().isBlank() || u.getNombreCompleto().length() > 100
      || !PATRON_SOLO_LETRAS.matcher(u.getNombreCompleto()).matches()) {
      return "nombre completo (solo letras, máximo 100 caracteres)";
    }
    if (u.getFechaNacimiento() == null || u.getFechaNacimiento().isAfter(LocalDate.now())) {
      return "fecha de nacimiento (formato dd/mm/aaaa, no puede ser posterior a hoy)";
    }
    if (u.getNacionalidad() == null || u.getNacionalidad().isBlank() || u.getNacionalidad().length() > 50) {
      return "nacionalidad (obligatoria, máximo 50 caracteres)";
    }
    if (u.getCorreo() == null || u.getCorreo().length() > 100 || !PATRON_CORREO.matcher(u.getCorreo().trim()).matches()) {
      return "correo electrónico (formato válido, máximo 100 caracteres)";
    }
    // Validar código de área (ejemplo: solo números, exactamente 3 dígitos o 1 a 3 dígitos según tu regla)
    if (u.getCodigoArea() == null || u.getCodigoArea().length() != 3 || !PATRON_NUMERICO.matcher(u.getCodigoArea()).matches()) {
      return "código de área (solo números, exactamente 3 dígitos)";
    }
    if (u.getTelefono() == null || u.getTelefono().length() != 8 || !PATRON_NUMERICO.matcher(u.getTelefono()).matches()) {
      return "teléfono (exactamente 8 dígitos)";
    }
    if (u.getDireccion() == null || u.getDireccion().isBlank() || u.getDireccion().length() > 150) {
      return "dirección (obligatoria, máximo 150 caracteres)";
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

    public Usuario save(Usuario usuario) {
        return usuarioRepository.save(usuario);
    }

    public void deleteById(Integer id) {
        usuarioRepository.deleteById(id);
    }

    // CU-01: crea la cuenta cifrando la contrasena antes de guardar.
    public Usuario registrar(Usuario usuario, String passwordPlano) {
        // FA01: campos obligatorios incompletos o invalidos
        if (primerCampoInvalido(usuario) != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Debe completar todos los datos personales obligatorios antes de continuar");
        }
        // FA01.1: correo ya registrado
        if (usuarioRepository.existsByCorreo(usuario.getCorreo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo electrónico ya se encuentra registrado");
        }
        // FA02: formato de la contrasena
        validarFormatoPassword(passwordPlano, MSG_FORMATO_PASSWORD_CU01);
        usuario.setContrasenaHash(passwordEncoder.encode(passwordPlano));
        return usuarioRepository.save(usuario);
    }

    // CU-00: valida credenciales sin revelar cual campo fallo (FA05).
    public Usuario autenticar(String correo, String passwordPlano) {
        Usuario usuario = usuarioRepository.findByCorreo(correo == null ? "" : correo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Correo electrónico o contraseña incorrectos"));

        if (passwordPlano == null || !passwordEncoder.matches(passwordPlano, usuario.getContrasenaHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Correo electrónico o contraseña incorrectos");
        }
        return usuario;
    }

    // CU-03, FA02: el correo nuevo no debe pertenecer a otra cuenta.
    public void validarCorreoDisponibleParaOtroUsuario(String correoNuevo, Integer idUsuarioActual) {
        usuarioRepository.findByCorreo(correoNuevo).ifPresent(existente -> {
            if (!existente.getIdUsuario().equals(idUsuarioActual)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo electrónico ya se encuentra registrado");
            }
        });
    }

    // CU-03: valida los datos (FA03: indica el campo con error) antes de modificar la entidad.
    public void validarDatosPerfil(Usuario datosNuevos) {
        String campo = primerCampoInvalido(datosNuevos);
        if (campo != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Formato de datos no válido. Revise el campo: " + campo);
        }
    }

    // CU-03: actualiza datos y, opcionalmente, la contrasena (exige la actual, FA01).
    public Usuario actualizarPerfil(Usuario usuario, String passwordActualPlano, String passwordNuevaPlano) {
        if (passwordNuevaPlano != null && !passwordNuevaPlano.isBlank()) {
            if (passwordActualPlano == null || !passwordEncoder.matches(passwordActualPlano, usuario.getContrasenaHash())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Contraseña actual incorrecta");
            }
            validarFormatoPassword(passwordNuevaPlano, MSG_FORMATO_PASSWORD_CU02);
            usuario.setContrasenaHash(passwordEncoder.encode(passwordNuevaPlano));
        }
        return usuarioRepository.save(usuario);
    }

    // CU-02: aplica la nueva contrasena tras validar el codigo de recuperacion.
    public void restablecerPassword(Usuario usuario, String passwordNuevaPlano) {
        validarFormatoPassword(passwordNuevaPlano, MSG_FORMATO_PASSWORD_CU02);
        usuario.setContrasenaHash(passwordEncoder.encode(passwordNuevaPlano));
        usuarioRepository.save(usuario);
    }
}
