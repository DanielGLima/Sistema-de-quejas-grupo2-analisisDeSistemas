package com.sistemadequejas.controller;

import com.sistemadequejas.dto.CancelarCasoRequest;
import com.sistemadequejas.dto.EvaluarCasoRequest;
import com.sistemadequejas.model.*;
import com.sistemadequejas.service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/casos")
public class CasoController {

    private static final String SESSION_ID_USUARIO = "idUsuario";

    @Autowired
    private CasoService casoService;

    @Autowired
    private EvidenciaCasoService evidenciaCasoService;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private TipoCasoService tipoCasoService;

    @Autowired
    private CategoriaCasoService categoriaCasoService;

    @Autowired
    private SucursalService sucursalService;

    @Autowired
    private EstadoCasoService estadoCasoService;

    @Autowired
    private BitacoraAuditoriaService bitacoraAuditoriaService;

    @Autowired
    private EmailService emailService;

    // FA02: formato alfanumerico simple (letras, numeros y guiones).
    private static final java.util.regex.Pattern PATRON_FACTURA = java.util.regex.Pattern.compile("^[A-Za-z0-9-]+$");

    // CU-06 (soporte minimo): lista los casos del usuario autenticado.
    @GetMapping("/mios")
    public List<Caso> misCasos(HttpSession session) {
        Usuario usuario = usuarioAutenticado(session);
        return casoService.findByUsuario(usuario);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Caso> obtener(@PathVariable Integer id, HttpSession session) {
        Usuario usuario = usuarioAutenticado(session);
        return casoService.findById(id)
                .filter(caso -> caso.getUsuario().getIdUsuario().equals(usuario.getIdUsuario()))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // CU-05: registra un nuevo caso. Las evidencias son obligatorias (al menos un archivo).
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Caso> crear(
            @RequestParam Integer idTipoCaso,
            @RequestParam Integer idSucursal,
            @RequestParam(required = false) Integer idCategoria,
            @RequestParam String descripcion,
            @RequestParam(required = false) String numeroFactura,
            @RequestParam(required = false) String nombreEmpleadoInvolucrado,
            @RequestParam(defaultValue = "false") boolean esAnonimo,
            @RequestParam(value = "archivos", required = false) List<MultipartFile> archivos,
            HttpSession session) {

        Usuario usuario = usuarioAutenticado(session);

        // FA01: campos obligatorios (descripcion 10-1000 caracteres, al menos una evidencia).
        if (descripcion == null || descripcion.trim().length() < 10 || archivos == null || archivos.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe ingresar los campos obligatorios");
        }

        // FA02: formato de numero de factura (campo opcional).
        if (numeroFactura != null && !numeroFactura.isBlank() && !PATRON_FACTURA.matcher(numeroFactura).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El formato de la factura es invalido");
        }

        TipoCaso tipoCaso = tipoCasoService.findById(idTipoCaso)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe ingresar los campos obligatorios"));
        Sucursal sucursal = sucursalService.findById(idSucursal)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe ingresar los campos obligatorios"));
        CategoriaCaso categoriaCaso = idCategoria != null
                ? categoriaCasoService.findById(idCategoria).orElse(null)
                : null;
        EstadoCaso estadoNuevo = estadoCasoService.findByNombre("Nuevo")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Catalogo de estados no inicializado"));

        Caso caso = new Caso();
        caso.setUsuario(usuario);
        caso.setSucursal(sucursal);
        caso.setTipoCaso(tipoCaso);
        caso.setCategoriaCaso(categoriaCaso);
        caso.setEstadoCaso(estadoNuevo);
        caso.setDescripcion(descripcion);
        caso.setNumeroFactura(numeroFactura);
        caso.setNombreEmpleadoInvolucrado(nombreEmpleadoInvolucrado);
        caso.setEsAnonimo(esAnonimo);

        Caso guardado = casoService.registrarCaso(caso);

        for (MultipartFile archivo : archivos) {
            String nombreArchivo = fileStorageService.guardar(archivo);

            EvidenciaCaso evidencia = new EvidenciaCaso();
            evidencia.setCaso(guardado);
            evidencia.setUrlArchivo("/uploads/" + nombreArchivo);
            evidencia.setTipoArchivo(nombreArchivo.substring(nombreArchivo.lastIndexOf('.') + 1));
            evidencia.setTamanoBytes((int) archivo.getSize());
            evidenciaCasoService.save(evidencia);
        }

        // Paso 7: notifica al usuario por correo con el numero de seguimiento (CU-14).
        emailService.enviar(
                usuario.getCorreo(),
                "Caso registrado - " + guardado.getIdentificadorVisible(),
                "Tu caso fue registrado exitosamente. Numero de seguimiento: " + guardado.getIdentificadorVisible()
                        + "\n\nPuedes consultar su estado desde 'Mis casos' en el Sistema de Quejas."
        );

        bitacoraAuditoriaService.registrarAccionUsuarioSobreCaso(usuario, guardado, "Registro de caso", "CU-05",
                "Caso " + guardado.getIdentificadorVisible() + " creado en estado Nuevo");

        return ResponseEntity.status(HttpStatus.CREATED).body(guardado);
    }

    // CU-07: cancela un caso propio del usuario autenticado.
    @PutMapping("/{id}/cancelar")
    public ResponseEntity<Caso> cancelar(@PathVariable Integer id, @RequestBody CancelarCasoRequest request, HttpSession session) {
        Usuario usuario = usuarioAutenticado(session);
        Caso caso = casoPropio(id, usuario);
        Caso cancelado = casoService.cancelar(caso, request.getMotivo());

        // CU-15: el cambio de estado queda en la bitacora de auditoria.
        bitacoraAuditoriaService.registrarAccionUsuarioSobreCaso(usuario, cancelado, "Cancelacion de caso", "CU-07",
                "Caso " + cancelado.getIdentificadorVisible() + " cancelado por el usuario. Motivo: " + request.getMotivo());

        return ResponseEntity.ok(cancelado);
    }

    // CU-08: registra la evaluacion de atencion de un caso propio del usuario autenticado.
    @PostMapping("/{id}/evaluacion")
    public ResponseEntity<EvaluacionCaso> evaluar(@PathVariable Integer id, @RequestBody EvaluarCasoRequest request, HttpSession session) {
        Usuario usuario = usuarioAutenticado(session);
        Caso caso = casoPropio(id, usuario);
        EvaluacionCaso evaluacion = casoService.evaluar(caso, request.getCalificacion(), request.getComentario());
        return ResponseEntity.status(HttpStatus.CREATED).body(evaluacion);
    }

    private Caso casoPropio(Integer id, Usuario usuario) {
        Caso caso = casoService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Caso no encontrado"));
        if (!caso.getUsuario().getIdUsuario().equals(usuario.getIdUsuario())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Caso no encontrado");
        }
        return caso;
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
