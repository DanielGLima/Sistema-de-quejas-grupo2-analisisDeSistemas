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
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/casos")
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
public class CasoController {

  private static final String SESSION_ID_USUARIO = "idUsuario";

  // CU-02 FA02: alfanumérico estricto sin símbolos (máximo 20 caracteres)
  private static final Pattern PATRON_FACTURA = Pattern.compile("^[A-Za-z0-9]{1,20}$");
  // CU-02 FA05: texto alfabético y espacios (máximo 100 caracteres)
  private static final Pattern PATRON_EMPLEADO = Pattern.compile("^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ ]{1,100}$");

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
  private EmailService emailService;

  // CU-03: lista los casos del usuario autenticado ("Mis casos")
  @GetMapping("/mios")
  public List<Caso> misCasos(HttpSession session) {
    Usuario usuario = usuarioAutenticado(session);
    return casoService.findByUsuario(usuario);
  }

  // CU-03: detalle completo con evidencias, historial de cambios de estado y respuestas oficiales.
  @GetMapping("/{id}")
  public ResponseEntity<CasoDetalleResponse> obtener(@PathVariable Integer id, HttpSession session) {
    Usuario usuario = usuarioAutenticado(session);
    return casoService.findById(id)
      .filter(caso -> caso.getUsuario().getIdUsuario().equals(usuario.getIdUsuario()))
      .map(caso -> ResponseEntity.ok(new CasoDetalleResponse(
        caso,
        evidenciaCasoService.findByCaso(caso),
        historialEstadoCasoService.findByCaso(caso),
        casoService.respuestasDe(caso)
      )))
      .orElseGet(() -> ResponseEntity.notFound().build());
  }

  // CU-02: registra un nuevo caso (Queja, Reclamo, Denuncia o Sugerencia)
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

    // FA01: campos obligatorios (tipo, sucursal, categoría, descripción 10-1000 y al menos una evidencia)
    if (idTipoCaso == null || idSucursal == null || idCategoria == null
      || descripcion == null || descripcion.trim().length() < 10 || descripcion.trim().length() > 1000
      || archivos == null || archivos.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe ingresar los campos obligatorios");
    }

    // FA02: formato de número de factura (campo opcional, alfanumérico sin símbolos)
    if (numeroFactura != null && !numeroFactura.isBlank() && !PATRON_FACTURA.matcher(numeroFactura.trim()).matches()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El formato de la factura es inválido");
    }

    // FA05: formato del nombre del empleado (campo opcional, solo letras y espacios)
    if (nombreEmpleadoInvolucrado != null && !nombreEmpleadoInvolucrado.isBlank()
      && !PATRON_EMPLEADO.matcher(nombreEmpleadoInvolucrado.trim()).matches()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre del empleado involucrado solo puede contener letras (máximo 100 caracteres)");
    }

    // FA03: validar tamaño y formato de cada archivo adjunto (máx 2 MB, solo JPG, PNG, PDF)
    for (MultipartFile arch : archivos) {
      if (arch.getSize() > 2 * 1024 * 1024) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo adjunto supera los 2 MB o no corresponde a un formato permitido (PDF/Imagen)");
      }
      String nombre = arch.getOriginalFilename() == null ? "" : arch.getOriginalFilename().toLowerCase();
      if (!nombre.endsWith(".jpg") && !nombre.endsWith(".jpeg") && !nombre.endsWith(".png") && !nombre.endsWith(".pdf")) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo adjunto supera los 2 MB o no corresponde a un formato permitido (PDF/Imagen)");
      }
    }

    TipoCaso tipoCaso = tipoCasoService.findById(idTipoCaso)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe ingresar los campos obligatorios"));
    Sucursal sucursal = sucursalService.findById(idSucursal)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe ingresar los campos obligatorios"));
    CategoriaCaso categoriaCaso = categoriaCasoService.findById(idCategoria)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe ingresar los campos obligatorios"));
    EstadoCaso estadoNuevo = estadoCasoService.findByNombre("Nuevo")
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Catálogo de estados no inicializado"));

    // FA04: anónimo solo se permite si el tipo de caso es Denuncia
    boolean esDenuncia = "Denuncia".equalsIgnoreCase(tipoCaso.getNombre());
    boolean anonimoFinal = esDenuncia && esAnonimo;

    Caso caso = new Caso();
    caso.setUsuario(usuario);
    caso.setSucursal(sucursal);
    caso.setTipoCaso(tipoCaso);
    caso.setCategoriaCaso(categoriaCaso);
    caso.setEstadoCaso(estadoNuevo);
    caso.setDescripcion(descripcion.trim());
    caso.setNumeroFactura(numeroFactura != null && !numeroFactura.isBlank() ? numeroFactura.trim() : null);
    caso.setNombreEmpleadoInvolucrado(nombreEmpleadoInvolucrado != null && !nombreEmpleadoInvolucrado.isBlank() ? nombreEmpleadoInvolucrado.trim() : null);
    caso.setEsAnonimo(anonimoFinal);

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

    // CU-10: notificar al usuario por correo con el número de seguimiento
    try {
      emailService.enviar(
        usuario.getCorreo(),
        "Caso registrado - " + guardado.getIdentificadorVisible(),
        "Tu caso fue registrado exitosamente. Número de seguimiento: " + guardado.getIdentificadorVisible()
          + "\n\nPuedes consultar su estado desde 'Mis casos' en el Sistema de Quejas."
      );
    } catch (Exception e) {
      System.err.println("Advertencia al enviar correo CU-10: " + e.getMessage());
    }

    // CU-10: bitácora de auditoría
    try {
      bitacoraAuditoriaService.registrarAccionUsuarioSobreCaso(usuario, guardado, "Registro de caso", "CU-02",
        "Caso " + guardado.getIdentificadorVisible() + " creado en estado Nuevo");
    } catch (Exception e) {
      System.err.println("Advertencia al auditar CU-10: " + e.getMessage());
    }

    return ResponseEntity.status(HttpStatus.CREATED).body(guardado);
  }

  // Cancelar caso propio
  @PutMapping("/{id}/cancelar")
  public ResponseEntity<Caso> cancelar(@PathVariable Integer id, @RequestBody CancelarCasoRequest request, HttpSession session) {
    Usuario usuario = usuarioAutenticado(session);
    Caso caso = casoPropio(id, usuario);
    Caso cancelado = casoService.cancelar(caso, request.getMotivo());

    bitacoraAuditoriaService.registrarAccionUsuarioSobreCaso(usuario, cancelado, "Cancelación de caso", "Gestión de casos",
      "Caso " + cancelado.getIdentificadorVisible() + " cancelado por el usuario. Motivo: " + request.getMotivo());

    return ResponseEntity.ok(cancelado);
  }

  // Evaluar caso resuelto o cerrado
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
