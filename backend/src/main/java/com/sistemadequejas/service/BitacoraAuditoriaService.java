package com.sistemadequejas.service;

import com.sistemadequejas.model.BitacoraAuditoria;
import com.sistemadequejas.model.Caso;
import com.sistemadequejas.model.Personal;
import com.sistemadequejas.model.Usuario;
import com.sistemadequejas.repository.BitacoraAuditoriaRepository;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// CU-15: registro transversal de acciones auditables. Los registros solo se crean
// y consultan; no existe ninguna operacion de edicion ni borrado.
@Service
public class BitacoraAuditoriaService {

    private static final Logger log = LoggerFactory.getLogger(BitacoraAuditoriaService.class);

    @Autowired
    private BitacoraAuditoriaRepository bitacoraAuditoriaRepository;

    public void registrarAccionUsuario(Usuario usuario, String accion, String moduloAfectado, String detalle) {
        registrar("Usuario", usuario, null, null, accion, moduloAfectado, detalle);
    }

    public void registrarAccionUsuarioSobreCaso(Usuario usuario, Caso caso, String accion, String moduloAfectado, String detalle) {
        registrar("Usuario", usuario, null, caso, accion, moduloAfectado, detalle);
    }

    public void registrarAccionPersonal(Personal personal, Caso caso, String accion, String moduloAfectado, String detalle) {
        registrar("Personal", null, personal == null ? null : personal.getIdPersonal(), caso, accion, moduloAfectado, detalle);
    }

    public void registrarAccionSistema(Caso caso, String accion, String moduloAfectado, String detalle) {
        registrar("Sistema", null, null, caso, accion, moduloAfectado, detalle);
    }

    // FA01/FA02: un fallo al auditar queda en el log tecnico y NO bloquea la operacion original.
    private void registrar(String tipoActor, Usuario usuario, Integer idPersonal, Caso caso, String accion, String moduloAfectado, String detalle) {
        try {
            BitacoraAuditoria registro = new BitacoraAuditoria();
            registro.setTipoActor(tipoActor);
            registro.setUsuario(usuario);
            registro.setIdPersonal(idPersonal);
            registro.setCaso(caso);
            registro.setAccion(recortar(accion, 50));
            registro.setModuloAfectado(recortar(moduloAfectado, 40));
            registro.setIpOrigen(recortar(ipDeLaPeticion(), 45));
            registro.setDetalle(recortar(detalle, 1500));
            bitacoraAuditoriaRepository.save(registro);
        } catch (Exception e) {
            log.error("No se pudo registrar en la bitacora de auditoria (accion: {}): {}", accion, e.getMessage());
        }
    }

    public List<BitacoraAuditoria> consultar(LocalDateTime desde, LocalDateTime hasta, String accion, String modulo, String tipoActor, int limite) {
        Specification<BitacoraAuditoria> spec = (root, query, cb) -> {
            List<Predicate> filtros = new ArrayList<>();
            if (desde != null) {
                filtros.add(cb.greaterThanOrEqualTo(root.get("fechaHora"), desde));
            }
            if (hasta != null) {
                filtros.add(cb.lessThanOrEqualTo(root.get("fechaHora"), hasta));
            }
            if (accion != null && !accion.isBlank()) {
                filtros.add(cb.like(cb.lower(root.get("accion")), "%" + accion.trim().toLowerCase() + "%"));
            }
            if (modulo != null && !modulo.isBlank()) {
                filtros.add(cb.like(cb.lower(root.get("moduloAfectado")), "%" + modulo.trim().toLowerCase() + "%"));
            }
            if (tipoActor != null && !tipoActor.isBlank()) {
                filtros.add(cb.equal(root.get("tipoActor"), tipoActor));
            }
            return cb.and(filtros.toArray(new Predicate[0]));
        };
        int tamano = Math.min(Math.max(limite, 1), 500);
        return bitacoraAuditoriaRepository
                .findAll(spec, PageRequest.of(0, tamano, Sort.by(Sort.Direction.DESC, "fechaHora")))
                .getContent();
    }

    // Junta los valores anteriores y nuevos en el formato JSON que exige CU-15.
    public static String cambio(String anteriorJson, String nuevoJson) {
        return "{\"anterior\":" + anteriorJson + ",\"nuevo\":" + nuevoJson + "}";
    }

    // Arma un JSON simple {"clave":"valor",...} para el detalle (valores anteriores / nuevos).
    public static String json(String... paresClaveValor) {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i + 1 < paresClaveValor.length; i += 2) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append("\"").append(escapar(paresClaveValor[i])).append("\":\"")
                    .append(escapar(paresClaveValor[i + 1])).append("\"");
        }
        return sb.append("}").toString();
    }

    private static String escapar(String valor) {
        if (valor == null) {
            return "";
        }
        return valor.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", " ");
    }

    private String ipDeLaPeticion() {
        RequestAttributes atributos = RequestContextHolder.getRequestAttributes();
        if (atributos instanceof ServletRequestAttributes servlet) {
            return servlet.getRequest().getRemoteAddr();
        }
        return null;
    }

    private String recortar(String texto, int max) {
        if (texto == null) {
            return null;
        }
        return texto.length() <= max ? texto : texto.substring(0, max);
    }
}
