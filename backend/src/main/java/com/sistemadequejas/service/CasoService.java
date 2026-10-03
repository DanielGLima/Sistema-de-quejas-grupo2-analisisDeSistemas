package com.sistemadequejas.service;

import com.sistemadequejas.config.SesionPersonal;
import com.sistemadequejas.model.*;
import com.sistemadequejas.repository.CasoRepository;
import com.sistemadequejas.repository.ReaperturaCasoRepository;
import com.sistemadequejas.repository.ReasignacionCasoRepository;
import com.sistemadequejas.repository.RespuestaCasoRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class CasoService {

    private static final Set<String> ESTADOS_CANCELABLES = Set.of("Nuevo", "En espera");
    private static final Set<String> ESTADOS_EVALUABLES = Set.of("Resuelto", "Cerrado");
    public static final Set<String> ESTADOS_FINALES = Set.of("Cerrado", "Cancelado por el usuario");
    private static final long PLAZO_REAPERTURA_DIAS = 15;

    // CU-10, FA01: flujo valido de estados (el caso "Cerrado" solo se reabre por solicitud del usuario, CU-09).
    private static final Map<String, List<String>> TRANSICIONES = Map.of(
            "Nuevo", List.of("En espera", "En Proceso", "Cerrado"),
            "En espera", List.of("En Proceso", "Cerrado"),
            "En Proceso", List.of("En espera", "Resuelto", "Cerrado"),
            "Resuelto", List.of("En Proceso", "Cerrado"),
            "Reapertura solicitada", List.of("En Proceso", "Cerrado")
    );

    @Autowired
    private CasoRepository casoRepository;

    @Autowired
    private EstadoCasoService estadoCasoService;

    @Autowired
    private HistorialEstadoCasoService historialEstadoCasoService;

    @Autowired
    private EvaluacionCasoService evaluacionCasoService;

    @Autowired
    private PersonalService personalService;

    @Autowired
    private NotificacionService notificacionService;

    @Autowired
    private BitacoraAuditoriaService bitacoraAuditoriaService;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private RespuestaCasoRepository respuestaCasoRepository;

    @Autowired
    private ReaperturaCasoRepository reaperturaCasoRepository;

    @Autowired
    private ReasignacionCasoRepository reasignacionCasoRepository;

    public List<Caso> findAll() {
        return casoRepository.findAll();
    }

    public List<Caso> findByUsuario(Usuario usuario) {
        return casoRepository.findByUsuarioOrderByFechaCreacionDesc(usuario);
    }

    public Optional<Caso> findById(Integer id) {
        return casoRepository.findById(id);
    }

    public void deleteById(Integer id) {
        casoRepository.deleteById(id);
    }

    // CU-05: guarda el caso y arma su identificador visible (prefijo tipo +
    // prefijo categoria + id interno), ej. QUE-COM-123 o DEN-124.
    public Caso registrarCaso(Caso caso) {
        String marcador = "TMP" + (System.nanoTime() % 100000000L);
        caso.setIdentificadorVisible(marcador);
        Caso guardado = casoRepository.save(caso);

        StringBuilder identificador = new StringBuilder(guardado.getTipoCaso().getCodigo());
        if (guardado.getCategoriaCaso() != null) {
            identificador.append("-").append(guardado.getCategoriaCaso().getCodigo());
        }
        identificador.append("-").append(guardado.getIdCaso());

        guardado.setIdentificadorVisible(identificador.toString());
        return casoRepository.save(guardado);
    }

    // CU-07: cancela un caso propio, solo si aun no inicio atencion.
    public Caso cancelar(Caso caso, String motivo) {
        // Paso 5: motivo opcional, maximo 250 caracteres.
        if (motivo != null && motivo.length() > 250) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El motivo de cancelación no puede superar los 250 caracteres");
        }
        String estadoActual = caso.getEstadoCaso().getNombre();
        if (!ESTADOS_CANCELABLES.contains(estadoActual) || caso.getPersonalAsignado() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El caso ya se encuentra en atención y no puede ser cancelado");
        }

        EstadoCaso estadoAnterior = caso.getEstadoCaso();
        EstadoCaso estadoCancelado = estadoPorNombre("Cancelado por el usuario");

        caso.setEstadoCaso(estadoCancelado);
        caso.setFechaActualizacion(LocalDateTime.now());
        Caso guardado = casoRepository.save(caso);

        historialEstadoCasoService.registrarCambio(guardado, estadoAnterior, estadoCancelado, motivo);

        // Paso 10: notifica al personal administrativo asignado (CU-14).
        notificacionService.notificarPersonalDelCaso(guardado, "CASO_CANCELADO",
                "Caso cancelado - " + guardado.getIdentificadorVisible(),
                "El usuario canceló el caso " + guardado.getIdentificadorVisible()
                        + (motivo == null || motivo.isBlank() ? "." : ". Motivo: " + motivo.trim()));
        return guardado;
    }

    // CU-08: registra la evaluacion de atencion de un caso resuelto/cerrado, una sola vez.
    public EvaluacionCaso evaluar(Caso caso, Short calificacion, String comentario) {
        if (!ESTADOS_EVALUABLES.contains(caso.getEstadoCaso().getNombre())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Solo se pueden evaluar casos en estado Resuelto o Cerrado");
        }
        if (caso.getEvaluacion() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Este caso ya cuenta con una evaluación registrada");
        }
        if (calificacion == null || calificacion < 1 || calificacion > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Debe seleccionar una calificación para continuar");
        }

        if (comentario != null && comentario.length() > 500) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El comentario no puede superar los 500 caracteres");
        }

        EvaluacionCaso evaluacion = new EvaluacionCaso();
        evaluacion.setCaso(caso);
        evaluacion.setCalificacion(calificacion);
        evaluacion.setComentario(comentario);
        return evaluacionCasoService.save(evaluacion);
    }

    // ------------------------------------------------------------------
    // CU-09: solicitar reapertura (usuario propietario)
    // ------------------------------------------------------------------
    public Caso solicitarReapertura(Caso caso, Usuario usuario, String motivo, MultipartFile evidencia) {
        if (!"Cerrado".equals(caso.getEstadoCaso().getNombre())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Solo se puede solicitar la reapertura de un caso en estado Cerrado");
        }

        // FA02: motivo obligatorio (10 a 500 caracteres).
        String motivoLimpio = motivo == null ? "" : motivo.trim();
        if (motivoLimpio.length() < 10 || motivoLimpio.length() > 500) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Debe ingresar los campos obligatorios: Motivo de reapertura");
        }

        // FA01 / paso 7: plazo de 15 dias posteriores al cierre.
        LocalDateTime fechaCierre = fechaDeCierre(caso);
        if (Duration.between(fechaCierre, LocalDateTime.now()).toDays() > PLAZO_REAPERTURA_DIAS) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El plazo para solicitar la reapertura ha vencido. Puede registrar un nuevo caso (CU-05)");
        }

        String urlEvidencia = null;
        if (evidencia != null && !evidencia.isEmpty()) {
            urlEvidencia = "/uploads/" + fileStorageService.guardar(evidencia);
        }

        EstadoCaso estadoAnterior = caso.getEstadoCaso();
        EstadoCaso estadoReapertura = estadoPorNombre("Reapertura solicitada");
        caso.setEstadoCaso(estadoReapertura);
        caso.setFechaActualizacion(LocalDateTime.now());
        Caso guardado = casoRepository.save(caso);

        ReaperturaCaso reapertura = new ReaperturaCaso();
        reapertura.setCaso(guardado);
        reapertura.setMotivo(motivoLimpio);
        reapertura.setUrlEvidencia(urlEvidencia);
        reaperturaCasoRepository.save(reapertura);

        // El historial previo se conserva: solo se agrega un nuevo cambio de estado.
        historialEstadoCasoService.registrarCambio(guardado, estadoAnterior, estadoReapertura, "Reapertura solicitada: " + motivoLimpio);

        bitacoraAuditoriaService.registrarAccionUsuarioSobreCaso(usuario, guardado, "Solicitud de reapertura", "Reapertura de casos",
                BitacoraAuditoriaService.cambio(
                        BitacoraAuditoriaService.json("caso", guardado.getIdentificadorVisible(), "estado", estadoAnterior.getNombre()),
                        BitacoraAuditoriaService.json("caso", guardado.getIdentificadorVisible(), "estado", estadoReapertura.getNombre(), "motivo", motivoLimpio)));

        // Paso 9: notifica al personal asignado (o, si no hay, a los Administradores Generales).
        String asunto = "Reapertura solicitada - " + guardado.getIdentificadorVisible();
        String cuerpo = "El usuario solicitó la reapertura del caso " + guardado.getIdentificadorVisible() + ". Motivo: " + motivoLimpio;
        notificacionService.notificarPersonalDelCaso(guardado, "REAPERTURA_SOLICITADA", asunto, cuerpo);
        return guardado;
    }

    private LocalDateTime fechaDeCierre(Caso caso) {
        return historialEstadoCasoService.findByCaso(caso).stream()
                .filter(h -> "Cerrado".equals(h.getEstadoNuevo().getNombre()))
                .map(HistorialEstadoCaso::getFechaCambio)
                .findFirst()
                .orElse(caso.getFechaActualizacion());
    }

    // ------------------------------------------------------------------
    // CU-10: gestionar estado y responsable
    // ------------------------------------------------------------------
    public static List<String> estadosPermitidos(String estadoActual) {
        return TRANSICIONES.getOrDefault(estadoActual, List.of());
    }

    // FA02: el Administrador General ve todo; el resto solo su sucursal.
    public void validarAlcance(Personal actor, Caso caso) {
        if (PersonalService.ROL_ADMINISTRADOR.equals(actor.getRol().getNombre())) {
            return;
        }
        boolean mismaSucursal = actor.getSucursal() != null
                && actor.getSucursal().getIdSucursal().equals(caso.getSucursal().getIdSucursal());
        if (!mismaSucursal) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, SesionPersonal.SIN_PERMISOS);
        }
    }

    public Caso gestionar(Caso caso, Personal actor, Integer idPersonalAsignado, String nuevoEstado, String notas, LocalDateTime fechaActualizacionVista) {
        validarAlcance(actor, caso);
        validarVersion(caso, fechaActualizacionVista);

        if (nuevoEstado == null || nuevoEstado.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe seleccionar un estado válido");
        }
        if (notas != null && notas.length() > 500) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Las observaciones no pueden superar los 500 caracteres");
        }

        EstadoCaso estadoAnterior = caso.getEstadoCaso();
        EstadoCaso estadoNuevo = estadoPorNombre(nuevoEstado.trim());
        boolean cambiaEstado = !estadoAnterior.getIdEstado().equals(estadoNuevo.getIdEstado());

        // FA01: transicion no permitida; se informan los estados validos.
        if (cambiaEstado) {
            List<String> permitidos = estadosPermitidos(estadoAnterior.getNombre());
            if (!permitidos.contains(estadoNuevo.getNombre())) {
                String detalle = permitidos.isEmpty() ? "ninguno" : String.join(", ", permitidos);
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Cambio de estado no permitido. Estados permitidos: " + detalle);
            }
        }

        Personal responsableAnterior = caso.getPersonalAsignado();
        Personal responsableNuevo = responsableAnterior;
        if (idPersonalAsignado != null) {
            responsableNuevo = personalService.findById(idPersonalAsignado)
                    .filter(p -> Boolean.TRUE.equals(p.getActivo()))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "El empleado responsable seleccionado no es válido"));
        }
        boolean cambiaResponsable = responsableNuevo != null
                && (responsableAnterior == null || !responsableAnterior.getIdPersonal().equals(responsableNuevo.getIdPersonal()));

        caso.setEstadoCaso(estadoNuevo);
        caso.setPersonalAsignado(responsableNuevo);
        caso.setFechaActualizacion(LocalDateTime.now());
        Caso guardado = casoRepository.save(caso);

        HistorialEstadoCaso historial = historialEstadoCasoService.registrarCambio(guardado, estadoAnterior, estadoNuevo, notas);
        historial.setIdPersonal(actor.getIdPersonal());
        historialEstadoCasoService.guardar(historial);

        if (cambiaResponsable) {
            registrarReasignacion(guardado, responsableAnterior, responsableNuevo, actor, "Asignación desde la gestión del caso");
        }

        bitacoraAuditoriaService.registrarAccionPersonal(actor, guardado, "Gestión de caso", "Gestión de casos",
                BitacoraAuditoriaService.cambio(
                        BitacoraAuditoriaService.json("caso", guardado.getIdentificadorVisible(),
                                "estado", estadoAnterior.getNombre(), "responsable", nombreDe(responsableAnterior)),
                        BitacoraAuditoriaService.json("caso", guardado.getIdentificadorVisible(),
                                "estado", estadoNuevo.getNombre(), "responsable", nombreDe(responsableNuevo), "notas", notas)));

        if (cambiaEstado) {
            notificacionService.notificarUsuario(guardado, guardado.getUsuario(), "CAMBIO_ESTADO",
                    "Actualización de estado - Caso " + guardado.getIdentificadorVisible(),
                    "Su caso " + guardado.getIdentificadorVisible() + " cambió de estado a \"" + estadoNuevo.getNombre()
                            + "\". Restaurante Las Delicias está dando seguimiento a su caso.");
        }
        if (cambiaResponsable) {
            avisarAsignacion(guardado, responsableNuevo);
        }
        return guardado;
    }

    // Reasignacion explicita con motivo obligatorio (10 a 300 caracteres).
    public Caso reasignar(Caso caso, Personal actor, Integer idPersonalNuevo, String motivo, LocalDateTime fechaActualizacionVista) {
        validarAlcance(actor, caso);
        validarVersion(caso, fechaActualizacionVista);

        Personal anterior = caso.getPersonalAsignado();
        if (anterior == null || ESTADOS_FINALES.contains(caso.getEstadoCaso().getNombre())
                || "Resuelto".equals(caso.getEstadoCaso().getNombre())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Solo se puede reasignar un caso asignado que no esté Resuelto ni Cerrado");
        }
        if (idPersonalNuevo == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe seleccionar un nuevo empleado responsable");
        }
        String motivoLimpio = motivo == null ? "" : motivo.trim();
        if (motivoLimpio.length() < 10 || motivoLimpio.length() > 300) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Debe ingresar el motivo de la reasignación (mínimo 10 y máximo 300 caracteres)");
        }
        Personal nuevo = personalService.findById(idPersonalNuevo)
                .filter(p -> Boolean.TRUE.equals(p.getActivo()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "El empleado responsable seleccionado no es válido"));
        if (nuevo.getIdPersonal().equals(anterior.getIdPersonal())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nuevo responsable debe ser distinto al actual");
        }

        caso.setPersonalAsignado(nuevo);
        caso.setFechaActualizacion(LocalDateTime.now());
        Caso guardado = casoRepository.save(caso);

        registrarReasignacion(guardado, anterior, nuevo, actor, motivoLimpio);

        bitacoraAuditoriaService.registrarAccionPersonal(actor, guardado, "Reasignación de caso", "Gestión de casos",
                BitacoraAuditoriaService.cambio(
                        BitacoraAuditoriaService.json("caso", guardado.getIdentificadorVisible(), "responsable", nombreDe(anterior)),
                        BitacoraAuditoriaService.json("caso", guardado.getIdentificadorVisible(), "responsable", nombreDe(nuevo), "motivo", motivoLimpio)));
        avisarAsignacion(guardado, nuevo);
        return guardado;
    }

    private void registrarReasignacion(Caso caso, Personal anterior, Personal nuevo, Personal ejecutor, String motivo) {
        ReasignacionCaso reasignacion = new ReasignacionCaso();
        reasignacion.setCaso(caso);
        reasignacion.setPersonalAnterior(anterior);
        reasignacion.setPersonalNuevo(nuevo);
        reasignacion.setPersonalEjecutor(ejecutor);
        reasignacion.setMotivo(motivo);
        reasignacionCasoRepository.save(reasignacion);
    }

    private void avisarAsignacion(Caso caso, Personal responsable) {
        notificacionService.notificarPersonal(caso, responsable, "REASIGNACION",
                "Asignación de caso - " + caso.getIdentificadorVisible(),
                "Se le asignó la gestión del caso " + caso.getIdentificadorVisible() + " para su atención y resolución.");
    }

    // FA03: el caso fue modificado por otra persona desde que se cargo la pantalla.
    private void validarVersion(Caso caso, LocalDateTime fechaVista) {
        if (fechaVista == null) {
            return;
        }
        LocalDateTime actual = caso.getFechaActualizacion().truncatedTo(ChronoUnit.MILLIS);
        if (!actual.equals(fechaVista.truncatedTo(ChronoUnit.MILLIS))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El caso fue actualizado por otro usuario. Recargue la información antes de realizar nuevos cambios");
        }
    }

    // ------------------------------------------------------------------
    // CU-11: responder caso
    // ------------------------------------------------------------------
    public RespuestaCaso responder(Caso caso, Personal actor, String titulo, String cuerpo, String acciones) {
        validarAlcance(actor, caso);

        // FA03: solo casos "En Proceso".
        if (!"En Proceso".equals(caso.getEstadoCaso().getNombre())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El caso debe encontrarse en un estado válido para poder responder");
        }
        if (caso.getPersonalAsignado() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El caso debe estar asignado a un responsable para poder responder");
        }
        boolean esOperador = PersonalService.ROL_OPERADOR.equals(actor.getRol().getNombre());
        if (esOperador && !caso.getPersonalAsignado().getIdPersonal().equals(actor.getIdPersonal())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, SesionPersonal.SIN_PERMISOS);
        }

        // FA02: informacion insuficiente, indicando los datos faltantes.
        List<String> faltantes = new ArrayList<>();
        if (titulo == null || titulo.isBlank() || titulo.length() > 100) {
            faltantes.add("título o resumen (máximo 100 caracteres)");
        }
        if (cuerpo == null || cuerpo.trim().length() < 20 || cuerpo.length() > 1000) {
            faltantes.add("cuerpo de la respuesta (entre 20 y 1000 caracteres)");
        }
        if (acciones != null && acciones.length() > 300) {
            faltantes.add("acciones de compensación/seguimiento (máximo 300 caracteres)");
        }
        if (!faltantes.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Debe completar la información requerida: " + String.join("; ", faltantes));
        }

        RespuestaCaso respuesta = new RespuestaCaso();
        respuesta.setCaso(caso);
        respuesta.setPersonal(actor);
        respuesta.setTitulo(titulo.trim());
        respuesta.setContenido(cuerpo.trim());
        respuesta.setAccionesSeguimiento(acciones == null || acciones.isBlank() ? null : acciones.trim());

        // FA01: la respuesta de un Operador requiere el visto bueno del Gerente.
        if (esOperador) {
            respuesta.setEstadoAprobacion(RespuestaCaso.PENDIENTE_APROBACION);
            RespuestaCaso guardada = respuestaCasoRepository.save(respuesta);
            bitacoraAuditoriaService.registrarAccionPersonal(actor, caso, "Respuesta pendiente de aprobación", "Respuesta de casos",
                    BitacoraAuditoriaService.cambio("{}", BitacoraAuditoriaService.json(
                            "caso", caso.getIdentificadorVisible(), "titulo", guardada.getTitulo(), "estadoRespuesta", RespuestaCaso.PENDIENTE_APROBACION)));
            return guardada;
        }

        RespuestaCaso guardada = respuestaCasoRepository.save(respuesta);
        marcarResuelto(caso, actor, guardada);
        return guardada;
    }

    public RespuestaCaso aprobarRespuesta(RespuestaCaso respuesta, Personal actor) {
        Caso caso = respuesta.getCaso();
        validarAlcance(actor, caso);

        if (!RespuestaCaso.PENDIENTE_APROBACION.equals(respuesta.getEstadoAprobacion())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La respuesta no está pendiente de aprobación");
        }
        if (!"En Proceso".equals(caso.getEstadoCaso().getNombre())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El caso debe encontrarse en un estado válido para poder responder");
        }

        respuesta.setEstadoAprobacion(RespuestaCaso.APROBADA);
        RespuestaCaso guardada = respuestaCasoRepository.save(respuesta);
        marcarResuelto(caso, actor, guardada);
        return guardada;
    }

    private void marcarResuelto(Caso caso, Personal actor, RespuestaCaso respuesta) {
        EstadoCaso anterior = caso.getEstadoCaso();
        EstadoCaso resuelto = estadoPorNombre("Resuelto");
        caso.setEstadoCaso(resuelto);
        caso.setFechaActualizacion(LocalDateTime.now());
        Caso guardado = casoRepository.save(caso);

        HistorialEstadoCaso historial = historialEstadoCasoService.registrarCambio(guardado, anterior, resuelto,
                "Respuesta oficial: " + respuesta.getTitulo());
        historial.setIdPersonal(actor.getIdPersonal());
        historialEstadoCasoService.guardar(historial);

        bitacoraAuditoriaService.registrarAccionPersonal(actor, guardado, "Respuesta oficial", "Respuesta de casos",
                BitacoraAuditoriaService.cambio(
                        BitacoraAuditoriaService.json("caso", guardado.getIdentificadorVisible(), "estado", anterior.getNombre()),
                        BitacoraAuditoriaService.json("caso", guardado.getIdentificadorVisible(), "estado", resuelto.getNombre(),
                                "titulo", respuesta.getTitulo(), "compensacion", respuesta.getAccionesSeguimiento())));

        notificacionService.notificarUsuario(guardado, guardado.getUsuario(), "RESPUESTA_OFICIAL",
                "Respuesta oficial - Caso " + guardado.getIdentificadorVisible(),
                "Se emitió una respuesta oficial sobre su caso " + guardado.getIdentificadorVisible() + ": \""
                        + respuesta.getTitulo() + "\". Ingrese al portal para ver los detalles.");
    }

    public List<RespuestaCaso> respuestasDe(Caso caso) {
        return respuestaCasoRepository.findByCasoOrderByFechaRespuestaDesc(caso);
    }

    public List<ReaperturaCaso> reaperturasDe(Caso caso) {
        return reaperturaCasoRepository.findByCasoOrderByFechaSolicitudDesc(caso);
    }

    public List<ReasignacionCaso> reasignacionesDe(Caso caso) {
        return reasignacionCasoRepository.findByCasoOrderByFechaReasignacionDesc(caso);
    }

    public Optional<RespuestaCaso> findRespuesta(Integer idRespuesta) {
        return respuestaCasoRepository.findById(idRespuesta);
    }

    // ------------------------------------------------------------------
    // CU-12: buscar y filtrar (criterios combinables, acotado por rol y sucursal)
    // ------------------------------------------------------------------
    public List<Caso> buscar(Personal actor, LocalDateTime desde, LocalDateTime hasta, Integer idTipoCaso, Integer idCategoria,
                             Integer idSucursal, Integer idEstado, String identificador, String correoCliente,
                             Integer idResponsable, String texto, String ordenarPor, boolean ascendente) {

        Specification<Caso> spec = (root, query, cb) -> {
            List<Predicate> filtros = new ArrayList<>();

            if (!PersonalService.ROL_ADMINISTRADOR.equals(actor.getRol().getNombre())) {
                Integer sucursalActor = actor.getSucursal() == null ? -1 : actor.getSucursal().getIdSucursal();
                filtros.add(cb.equal(root.get("sucursal").get("idSucursal"), sucursalActor));
            }
            if (desde != null) {
                filtros.add(cb.greaterThanOrEqualTo(root.get("fechaCreacion"), desde));
            }
            if (hasta != null) {
                filtros.add(cb.lessThanOrEqualTo(root.get("fechaCreacion"), hasta));
            }
            if (idTipoCaso != null) {
                filtros.add(cb.equal(root.get("tipoCaso").get("idTipoCaso"), idTipoCaso));
            }
            if (idCategoria != null) {
                filtros.add(cb.equal(root.get("categoriaCaso").get("idCategoria"), idCategoria));
            }
            if (idSucursal != null) {
                filtros.add(cb.equal(root.get("sucursal").get("idSucursal"), idSucursal));
            }
            if (idEstado != null) {
                filtros.add(cb.equal(root.get("estadoCaso").get("idEstado"), idEstado));
            }
            if (identificador != null && !identificador.isBlank()) {
                filtros.add(cb.like(cb.lower(root.get("identificadorVisible")), "%" + identificador.trim().toLowerCase() + "%"));
            }
            if (correoCliente != null && !correoCliente.isBlank()) {
                filtros.add(cb.like(cb.lower(root.get("usuario").get("correo")), "%" + correoCliente.trim().toLowerCase() + "%"));
            }
            if (idResponsable != null) {
                if (idResponsable < 0) {
                    filtros.add(cb.isNull(root.get("personalAsignado")));
                } else {
                    filtros.add(cb.equal(root.get("personalAsignado").get("idPersonal"), idResponsable));
                }
            }
            if (texto != null && !texto.isBlank()) {
                String patron = "%" + texto.trim().toLowerCase() + "%";
                filtros.add(cb.or(
                        cb.like(cb.lower(root.get("identificadorVisible")), patron),
                        cb.like(cb.lower(root.get("descripcion")), patron)));
            }
            return cb.and(filtros.toArray(new Predicate[0]));
        };

        String campo = switch (ordenarPor == null ? "" : ordenarPor) {
            case "estado" -> "estadoCaso.nombre";
            case "tipo" -> "tipoCaso.nombre";
            case "sucursal" -> "sucursal.nombreSucursal";
            case "identificador" -> "identificadorVisible";
            default -> "fechaCreacion";
        };
        Sort orden = Sort.by(ascendente ? Sort.Direction.ASC : Sort.Direction.DESC, campo);
        return casoRepository.findAll(spec, orden);
    }

    private EstadoCaso estadoPorNombre(String nombre) {
        return estadoCasoService.findByNombre(nombre)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "El estado indicado no existe en el catálogo"));
    }

    private String nombreDe(Personal personal) {
        return personal == null ? "Sin asignar" : personal.getNombreCompleto();
    }
}
