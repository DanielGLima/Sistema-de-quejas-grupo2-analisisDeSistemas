package com.sistemadequejas.controller;

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

    // CU-05: registra un nuevo caso con sus evidencias (opcionales).
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

        if (descripcion == null || descripcion.trim().length() < 10) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe ingresar los campos obligatorios");
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

        if (archivos != null) {
            for (MultipartFile archivo : archivos) {
                String nombreArchivo = fileStorageService.guardar(archivo);

                EvidenciaCaso evidencia = new EvidenciaCaso();
                evidencia.setCaso(guardado);
                evidencia.setUrlArchivo("/uploads/" + nombreArchivo);
                evidencia.setTipoArchivo(nombreArchivo.substring(nombreArchivo.lastIndexOf('.') + 1));
                evidencia.setTamanoBytes((int) archivo.getSize());
                evidenciaCasoService.save(evidencia);
            }
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(guardado);
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
