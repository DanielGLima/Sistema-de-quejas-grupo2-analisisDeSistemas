package com.sistemadequejas.service;

import com.sistemadequejas.model.Usuario;
import com.sistemadequejas.repository.NacionalidadRepository;
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
    private NacionalidadRepository nacionalidadRepository;

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

    // CU-01 FA01 / CU-03 FA03: valida los datos personales; devuelve el nombre del primer campo invalido o null.
    private String primerCampoInvalido(Usuario u) {
        if (u.getNombreCompleto() == null || u.getNombreCompleto().isBlank() || u.getNombreCompleto().length() > 100
                || !PATRON_SOLO_LETRAS.matcher(u.getNombreCompleto()).matches()) {
            return "nombre completo";
        }
        if (u.getFechaNacimiento() == null || u.getFechaNacimiento().isAfter(LocalDate.now())
                || u.getFechaNacimiento().isBefore(LocalDate.of(1900, 1, 1))) {
            return "fecha de nacimiento";
        }
        if (u.getNacionalidad() == null || u.getNacionalidad().isBlank() || u.getNacionalidad().length() > 50
                || !nacionalidadRepository.existsByNombreAndActivoTrue(u.getNacionalidad())) {
            return "nacionalidad";
        }
        if (u.getCorreo() == null || u.getCorreo().length() > 100 || !PATRON_CORREO.matcher(u.getCorreo().trim()).matches()) {
            return "correo electrónico";
        }
        if (u.getCodigoArea() == null || u.getCodigoArea().length() < 1 || u.getCodigoArea().length() > 4
                || !PATRON_NUMERICO.matcher(u.getCodigoArea()).matches()) {
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

    // CU-01 paso 4: datos personales completos y correo no registrado.
    public void validarDatosRegistro(Usuario usuario) {
        // FA01: campos obligatorios incompletos o invalidos
        if (primerCampoInvalido(usuario) != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Debe completar todos los datos personales obligatorios antes de continuar");
        }
        // FA01.1: correo ya registrado
        if (usuarioRepository.existsByCorreo(usuario.getCorreo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo electrónico ya se encuentra registrado");
        }
    }

    // Valida una contrasena nueva: longitud, formato y, si se envia, que coincida con su confirmacion.
    public void validarPasswordNueva(String password, String confirmacion, String mensajeFormato) {
        validarFormatoPassword(password, mensajeFormato);
        if (confirmacion != null && !confirmacion.equals(password)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Las contraseñas ingresadas no coinciden");
        }
    }

    // CU-01: crea la cuenta cifrando la contrasena antes de guardar.
    @Transactional
    public Usuario registrar(Usuario usuario, String passwordPlano, String confirmacionPassword) {
        validarDatosRegistro(usuario);
        // FA03 a FA05: longitud, formato y confirmacion de la contrasena
        validarPasswordNueva(passwordPlano, confirmacionPassword, MSG_FORMATO_PASSWORD_CU01);
        usuario.setContrasenaHash(passwordEncoder.encode(passwordPlano));
        return usuarioRepository.saveAndFlush(usuario);
    }

    // CU-00: valida credenciales sin revelar cual campo fallo (FA05).
    public Usuario autenticar(String correo, String passwordPlano) {
        Usuario usuario = usuarioRepository.findByCorreo(correo == null ? "" : correo.trim())
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
    @Transactional
    public Usuario actualizarPerfil(Usuario usuario, String passwordActualPlano, String passwordNuevaPlano) {
        validarDatosPerfil(usuario);
        if (passwordNuevaPlano != null && !passwordNuevaPlano.isBlank()) {
            // CU-01 FA19: contrasena actual vacia o incorrecta; FA21: formato de la nueva contrasena.
            if (passwordActualPlano == null || passwordActualPlano.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe ingresar su contraseña actual para poder cambiarla");
            }
            if (!passwordEncoder.matches(passwordActualPlano, usuario.getContrasenaHash())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Contraseña actual incorrecta");
            }
            if (passwordNuevaPlano.length() < 6 || passwordNuevaPlano.length() > 20
                    || !PATRON_PASSWORD.matcher(passwordNuevaPlano).matches()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Formato de datos no válido. Revise el campo: nueva contraseña");
            }
            usuario.setContrasenaHash(passwordEncoder.encode(passwordNuevaPlano));
        }
        return usuarioRepository.saveAndFlush(usuario);
    }

    // CU-02: aplica la nueva contrasena tras validar el codigo de recuperacion.
    @Transactional
    public void restablecerPassword(Usuario usuario, String passwordNuevaPlano) {
        validarFormatoPassword(passwordNuevaPlano, MSG_FORMATO_PASSWORD_CU02);
        usuario.setContrasenaHash(passwordEncoder.encode(passwordNuevaPlano));
        usuarioRepository.saveAndFlush(usuario);
    }
}
