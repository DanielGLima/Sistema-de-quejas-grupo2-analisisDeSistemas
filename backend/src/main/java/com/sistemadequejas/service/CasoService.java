package com.sistemadequejas.service;

import com.sistemadequejas.model.Caso;
import com.sistemadequejas.model.EstadoCaso;
import com.sistemadequejas.model.EvaluacionCaso;
import com.sistemadequejas.model.Usuario;
import com.sistemadequejas.repository.CasoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class CasoService {

    private static final Set<String> ESTADOS_CANCELABLES = Set.of("Nuevo", "En espera");
    private static final Set<String> ESTADOS_EVALUABLES = Set.of("Resuelto", "Cerrado");

    @Autowired
    private CasoRepository casoRepository;

    @Autowired
    private EstadoCasoService estadoCasoService;

    @Autowired
    private HistorialEstadoCasoService historialEstadoCasoService;

    @Autowired
    private EvaluacionCasoService evaluacionCasoService;

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
        String estadoActual = caso.getEstadoCaso().getNombre();
        if (!ESTADOS_CANCELABLES.contains(estadoActual) || caso.getIdPersonalAsignado() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El caso ya se encuentra en atencion y no puede ser cancelado");
        }

        EstadoCaso estadoAnterior = caso.getEstadoCaso();
        EstadoCaso estadoCancelado = estadoCasoService.findByNombre("Cancelado por el usuario")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Catalogo de estados no inicializado"));

        caso.setEstadoCaso(estadoCancelado);
        Caso guardado = casoRepository.save(caso);

        historialEstadoCasoService.registrarCambio(guardado, estadoAnterior, estadoCancelado, motivo);
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
                    "Este caso ya cuenta con una evaluacion registrada");
        }
        if (calificacion == null || calificacion < 1 || calificacion > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Debe seleccionar una calificacion para continuar");
        }

        EvaluacionCaso evaluacion = new EvaluacionCaso();
        evaluacion.setCaso(caso);
        evaluacion.setCalificacion(calificacion);
        evaluacion.setComentario(comentario);
        return evaluacionCasoService.save(evaluacion);
    }
}
