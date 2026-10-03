package com.sistemadequejas.controller;

import com.sistemadequejas.dto.CancelarCasoRequest;
import com.sistemadequejas.dto.CasoDetalleResponse;
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
    private HistorialEstadoCasoService historialEstadoCasoService;

    @Autowired
    private BitacoraAuditoriaService bitacoraAuditoriaService;

    @Autowired
    private NotificacionService notificacionService;

    // FA02: numero de factura alfanumerico (letras y numeros), maximo 20 caracteres.
    private static final java.util.regex.Pattern PATRON_FACTURA = java.util.regex.Pattern.compile("^[A-Za-z0-9]{1,20}$");
    // Nombre del empleado involucrado: texto alfabetico, maximo 100 caracteres.
    private static final java.util.regex.Pattern PATRON_NOMBRE = java.util.regex.Pattern.compile("^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ ]{1,100}$");

    // CU-06 (soporte minimo): lista los casos del usuario autenticado.
    @GetMapping("/mios")
    public List<Caso> misCasos(HttpSession session) {
        Usuario usuario = usuarioAutenticado(session);
        return casoService.findByUsuario(usuario);
    }

    // CU-06, paso 5: detalle completo con evidencias e historial de cambios de estado.
    @GetMapping("/{id}")
    public ResponseEntity<CasoDetalleResponse> obtener(@PathVariable Integer id, HttpSession session) {
        Usuario usuario = usuarioAutenticado(session);
        return casoService.findById(id)
                .filter(caso -> caso.getUsuario().getIdUsuario().equals(usuario.getIdUsuario()))
                .map(caso -> ResponseEntity.ok(new CasoDetalleResponse(
                        caso,
                        evidenciaCasoService.findByCaso(caso),
                        historialEstadoCasoService.findByCaso(caso),
                        casoService.respuestasDe(caso).stream()
                                .filter(r -> RespuestaCaso.APROBADA.equals(r.getEstadoAprobacion()))
                                .toList()
                )))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // CU-05: registra un nuevo caso. Las evidencias son obligatorias (al menos un archivo).
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Caso> crear(
            @RequestParam Integer idTipoCaso,
            @RequestParam Integer idSucursal,
            @RequestParam Integer idCategoria,
            @RequestParam String descripcion,
            @RequestParam(required = false) String numeroFactura,
            @RequestParam(required = false) String nombreEmpleadoInvolucrado,
            @RequestParam(defaultValue = "false") boolean esAnonimo,
            @RequestParam(value = "archivos", required = false) List<MultipartFile> archivos,
            HttpSession session) {

        Usuario usuario = usuarioAutenticado(session);

        // FA01: campos obligatorios (categoria del servicio, descripcion 10-1000 caracteres, al menos una evidencia).
        if (descripcion == null || descripcion.trim().length() < 10 || descripcion.length() > 1000
                || archivos == null || archivos.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe ingresar los campos obligatorios");
        }

        // FA02: formato de numero de factura (campo opcional).
        if (numeroFactura != null && !numeroFactura.isBlank() && !PATRON_FACTURA.matcher(numeroFactura).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El formato de la factura es inválido");
        }

        if (nombreEmpleadoInvolucrado != null && !nombreEmpleadoInvolucrado.isBlank()
                && !PATRON_NOMBRE.matcher(nombreEmpleadoInvolucrado).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El nombre del empleado involucrado solo puede contener letras (máximo 100 caracteres)");
        }

        // FA03: se validan TODOS los archivos antes de registrar nada (si uno falla, no se crea el caso).
        archivos.forEach(fileStorageService::validar);

        TipoCaso tipoCaso = tipoCasoService.findById(idTipoCaso)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe ingresar los campos obligatorios"));
        Sucursal sucursal = sucursalService.findById(idSucursal)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe ingresar los campos obligatorios"));
        CategoriaCaso categoriaCaso = categoriaCasoService.findById(idCategoria)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe ingresar los campos obligatorios"));
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
        // FA04: la denuncia anonima solo aplica al tipo Denuncia.
        caso.setEsAnonimo(esAnonimo && "Denuncia".equals(tipoCaso.getNombre()));

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
        notificacionService.notificarUsuario(
                guardado,
                usuario,
                "REGISTRO_CASO",
                "Caso registrado - " + guardado.getIdentificadorVisible(),
                "Tu caso fue registrado exitosamente. Número de seguimiento: " + guardado.getIdentificadorVisible()
                        + ". Puedes consultar su estado desde 'Mis casos' en el Sistema de Quejas."
        );

        // Paso 8: notifica al personal administrativo (CU-14).
        notificacionService.notificarPersonalDelCaso(guardado, "CASO_NUEVO",
                "Nuevo caso registrado - " + guardado.getIdentificadorVisible(),
                "Se registró un nuevo caso " + guardado.getIdentificadorVisible() + " (" + tipoCaso.getNombre()
                        + ") en la sucursal " + sucursal.getNombreSucursal() + ".");

        bitacoraAuditoriaService.registrarAccionUsuarioSobreCaso(usuario, guardado, "Registro de caso", "Registro de casos",
                BitacoraAuditoriaService.cambio("{}", BitacoraAuditoriaService.json(
                        "caso", guardado.getIdentificadorVisible(), "tipo", tipoCaso.getNombre(),
                        "sucursal", sucursal.getNombreSucursal(), "estado", "Nuevo")));

        return ResponseEntity.status(HttpStatus.CREATED).body(guardado);
    }

    // CU-07: cancela un caso propio del usuario autenticado.
    @PutMapping("/{id}/cancelar")
    public ResponseEntity<Caso> cancelar(@PathVariable Integer id, @RequestBody CancelarCasoRequest request, HttpSession session) {
        Usuario usuario = usuarioAutenticado(session);
        Caso caso = casoPropio(id, usuario);
        String estadoAntes = caso.getEstadoCaso().getNombre();
        Caso cancelado = casoService.cancelar(caso, request.getMotivo());

        // CU-15: el cambio de estado queda en la bitacora de auditoria.
        bitacoraAuditoriaService.registrarAccionUsuarioSobreCaso(usuario, cancelado, "Cancelación de caso", "Cancelación de casos",
                BitacoraAuditoriaService.cambio(
                        BitacoraAuditoriaService.json("caso", cancelado.getIdentificadorVisible(), "estado", estadoAntes),
                        BitacoraAuditoriaService.json("caso", cancelado.getIdentificadorVisible(), "estado", cancelado.getEstadoCaso().getNombre(),
                                "motivo", request.getMotivo())));

        return ResponseEntity.ok(cancelado);
    }

    // CU-08: registra la evaluacion de atencion de un caso propio del usuario autenticado.
    @PostMapping("/{id}/evaluacion")
    public ResponseEntity<EvaluacionCaso> evaluar(@PathVariable Integer id, @RequestBody EvaluarCasoRequest request, HttpSession session) {
        Usuario usuario = usuarioAutenticado(session);
        Caso caso = casoPropio(id, usuario);
        EvaluacionCaso evaluacion = casoService.evaluar(caso, request.getCalificacion(), request.getComentario());
        bitacoraAuditoriaService.registrarAccionUsuarioSobreCaso(usuario, caso, "Evaluación de atención", "Evaluación de casos",
                BitacoraAuditoriaService.cambio("{}", BitacoraAuditoriaService.json(
                        "caso", caso.getIdentificadorVisible(), "calificación", String.valueOf(request.getCalificacion()))));
        return ResponseEntity.status(HttpStatus.CREATED).body(evaluacion);
    }

    // CU-09: solicita la reapertura de un caso cerrado propio (evidencia opcional).
    @PostMapping(value = "/{id}/reapertura", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Caso> solicitarReapertura(
            @PathVariable Integer id,
            @RequestParam(required = false) String motivo,
            @RequestParam(value = "archivo", required = false) MultipartFile archivo,
            HttpSession session) {
        Usuario usuario = usuarioAutenticado(session);
        Caso caso = casoPropio(id, usuario);
        return ResponseEntity.ok(casoService.solicitarReapertura(caso, usuario, motivo, archivo));
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
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Debe iniciar sesión");
        }
        return usuarioService.findById((Integer) idUsuario)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Debe iniciar sesión"));
    }
}
