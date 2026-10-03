package com.sistemadequejas.service;

import com.sistemadequejas.model.Personal;
import com.sistemadequejas.model.Rol;
import com.sistemadequejas.model.Sucursal;
import com.sistemadequejas.repository.CasoRepository;
import com.sistemadequejas.repository.PersonalRepository;
import com.sistemadequejas.repository.RolRepository;
import com.sistemadequejas.repository.SucursalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

// CU-10 a CU-13: personal interno (Administrador General, Gerente, Operador).
@Service
public class PersonalService {

    public static final String ROL_ADMINISTRADOR = "Administrador General";
    public static final String ROL_GERENTE = "Gerente";
    public static final String ROL_OPERADOR = "Operador";

    private static final Pattern PATRON_CORREO = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern PATRON_SOLO_LETRAS = Pattern.compile("^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ ]+$");
    private static final Pattern PATRON_ALFANUMERICO = Pattern.compile("^[A-Za-z0-9]+$");

    @Autowired
    private PersonalRepository personalRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private SucursalRepository sucursalRepository;

    @Autowired
    private CasoRepository casoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public List<Personal> findAll() {
        return personalRepository.findAll();
    }

    public List<Personal> findActivos() {
        return personalRepository.findByActivoTrueOrderByNombreCompleto();
    }

    public Optional<Personal> findById(Integer id) {
        return personalRepository.findById(id);
    }

    public Optional<Personal> findByCorreo(String correo) {
        return personalRepository.findByCorreo(correo);
    }

    public long contar() {
        return personalRepository.count();
    }

    // Login del personal interno; mismo mensaje generico que CU-00 FA05.
    public Personal autenticar(String correo, String passwordPlano) {
        Personal personal = personalRepository.findByCorreo(correo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Correo electronico o contrasena incorrectos"));

        if (!Boolean.TRUE.equals(personal.getActivo())
                || passwordPlano == null
                || !passwordEncoder.matches(passwordPlano, personal.getContrasenaHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Correo electronico o contrasena incorrectos");
        }
        return personal;
    }

    // CU-13, paso 4: alta de usuario interno.
    public Personal crear(String nombreCompleto, String correo, Integer idRol, Integer idSucursal, Boolean activo, String passwordTemporal) {
        validarDatos(nombreCompleto, correo);
        if (personalRepository.existsByCorreo(correo)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo electronico ya se encuentra registrado");
        }
        validarPasswordTemporal(passwordTemporal);

        Personal personal = new Personal();
        personal.setNombreCompleto(nombreCompleto.trim());
        personal.setCorreo(correo.trim());
        personal.setRol(buscarRol(idRol));
        personal.setSucursal(buscarSucursal(idSucursal));
        personal.setActivo(activo == null || activo);
        personal.setContrasenaHash(passwordEncoder.encode(passwordTemporal));
        return personalRepository.save(personal);
    }

    // CU-13, paso 4 / FA02: edicion, activacion y desactivacion.
    public Personal actualizar(Personal personal, String nombreCompleto, String correo, Integer idRol, Integer idSucursal, Boolean activo, String passwordTemporal) {
        validarDatos(nombreCompleto, correo);
        personalRepository.findByCorreo(correo)
                .filter(otro -> !otro.getIdPersonal().equals(personal.getIdPersonal()))
                .ifPresent(otro -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo electronico ya se encuentra registrado");
                });

        boolean desactivando = Boolean.TRUE.equals(personal.getActivo()) && Boolean.FALSE.equals(activo);
        if (desactivando) {
            long casosActivos = casoRepository.countByPersonalAsignadoAndEstadoCasoNombreNotIn(personal, CasoService.ESTADOS_FINALES);
            if (casosActivos > 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "El usuario tiene " + casosActivos + " caso(s) activo(s) asignado(s). Debe reasignarlos antes de desactivar la cuenta");
            }
        }

        personal.setNombreCompleto(nombreCompleto.trim());
        personal.setCorreo(correo.trim());
        personal.setRol(buscarRol(idRol));
        personal.setSucursal(buscarSucursal(idSucursal));
        if (activo != null) {
            personal.setActivo(activo);
        }
        if (passwordTemporal != null && !passwordTemporal.isBlank()) {
            validarPasswordTemporal(passwordTemporal);
            personal.setContrasenaHash(passwordEncoder.encode(passwordTemporal));
        }
        return personalRepository.save(personal);
    }

    public Personal crearAdministradorInicial(String nombre, String correo, String passwordPlano) {
        Personal personal = new Personal();
        personal.setNombreCompleto(nombre);
        personal.setCorreo(correo);
        personal.setRol(rolRepository.findAll().stream()
                .filter(rol -> ROL_ADMINISTRADOR.equals(rol.getNombre()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("El catalogo de roles no esta inicializado")));
        personal.setContrasenaHash(passwordEncoder.encode(passwordPlano));
        return personalRepository.save(personal);
    }

    // FA03 (CU-13): datos invalidos, con el detalle de que corregir.
    private void validarDatos(String nombreCompleto, String correo) {
        if (nombreCompleto == null || nombreCompleto.isBlank() || nombreCompleto.length() > 100
                || !PATRON_SOLO_LETRAS.matcher(nombreCompleto).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Nombre completo invalido: solo letras, obligatorio y maximo 100 caracteres");
        }
        if (correo == null || correo.length() > 100 || !PATRON_CORREO.matcher(correo).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Correo electronico invalido: debe tener formato de correo y maximo 100 caracteres");
        }
    }

    private void validarPasswordTemporal(String password) {
        if (password == null || password.length() < 6 || password.length() > 20 || !PATRON_ALFANUMERICO.matcher(password).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Contrasena temporal invalida: alfanumerica, entre 6 y 20 caracteres");
        }
    }

    private Rol buscarRol(Integer idRol) {
        return rolRepository.findById(idRol == null ? -1 : idRol)
                .filter(rol -> !Boolean.FALSE.equals(rol.getActivo()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe seleccionar un rol valido"));
    }

    private Sucursal buscarSucursal(Integer idSucursal) {
        return sucursalRepository.findById(idSucursal == null ? -1 : idSucursal)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe seleccionar una sucursal valida"));
    }
}
