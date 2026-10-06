package com.sistemadequejas.service;

import com.sistemadequejas.model.Caso;
import com.sistemadequejas.model.Notificacion;
import com.sistemadequejas.model.Personal;
import com.sistemadequejas.model.Usuario;
import com.sistemadequejas.repository.NotificacionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

// CU-14: notificaciones automaticas por correo, con registro del resultado y reintentos.
@Service
public class NotificacionService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionService.class);
    private static final Pattern PATRON_CORREO = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final int MAX_REINTENTOS = 3;

    @Autowired
    private NotificacionRepository notificacionRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PersonalService personalService;

    public List<Notificacion> historial() {
        return notificacionRepository.findTop200ByOrderByFechaEnvioDesc();
    }

    public void notificarUsuario(Caso caso, Usuario usuario, String tipoEvento, String asunto, String cuerpo) {
        notificar(caso, usuario, null, usuario == null ? null : usuario.getCorreo(), tipoEvento, asunto, cuerpo);
    }

    public void notificarPersonal(Caso caso, Personal personal, String tipoEvento, String asunto, String cuerpo) {
        notificar(caso, null, personal, personal == null ? null : personal.getCorreo(), tipoEvento, asunto, cuerpo);
    }

    // Paso "notifica al personal administrativo asignado": el responsable del caso; si aun no
    // tiene, los Administradores Generales y los Gerentes de la sucursal del caso.
    public void notificarPersonalDelCaso(Caso caso, String tipoEvento, String asunto, String cuerpo) {
        List<Personal> destinatarios = new ArrayList<>();
        if (caso.getPersonalAsignado() != null) {
            destinatarios.add(caso.getPersonalAsignado());
        } else {
            for (Personal personal : personalService.findActivos()) {
                String rol = personal.getRol().getNombre();
                boolean esAdmin = PersonalService.ROL_ADMINISTRADOR.equals(rol);
                boolean esGerenteDeLaSucursal = PersonalService.ROL_GERENTE.equals(rol)
                        && personal.getSucursal() != null
                        && personal.getSucursal().getIdSucursal().equals(caso.getSucursal().getIdSucursal());
                if (esAdmin || esGerenteDeLaSucursal) {
                    destinatarios.add(personal);
                }
            }
        }
        for (Personal personal : destinatarios) {
            notificarPersonal(caso, personal, tipoEvento, asunto, cuerpo);
        }
    }

    // CU-14 paso 5: el mensaje incluye el codigo del caso y la fecha/hora del evento (DD/MM/AAAA HH:MM:SS), maximo 500.
    private String armarCuerpo(Caso caso, String cuerpo) {
        String pie = (caso == null ? "" : " | Caso: " + caso.getIdentificadorVisible())
                + " | Fecha y hora: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
        String principal = recortar(cuerpo, 500 - pie.length());
        return principal + pie;
    }

    // Nunca lanza excepcion: la operacion que origino la notificacion continua (FA01/FA02).
    private void notificar(Caso caso, Usuario usuario, Personal personal, String correo, String tipoEvento, String asunto, String cuerpo) {
        try {
            if (usuario == null && personal == null) {
                log.warn("Notificacion {} omitida: no hay destinatario asociado", tipoEvento);
                return;
            }

            Notificacion notificacion = new Notificacion();
            notificacion.setCaso(caso);
            notificacion.setUsuario(usuario);
            notificacion.setPersonal(personal);
            notificacion.setTipoEvento(recortar(tipoEvento, 50));
            notificacion.setAsunto(recortar(asunto, 100));
            notificacion.setContenido(armarCuerpo(caso, cuerpo));

            // FA02: sin medio de contacto valido se registra la incidencia y se continua.
            if (correo == null || !PATRON_CORREO.matcher(correo).matches()) {
                notificacion.setCorreoDestino("No disponible");
                notificacion.setEstadoEnvio(Notificacion.SIN_CONTACTO);
                notificacionRepository.save(notificacion);
                return;
            }

            notificacion.setCorreoDestino(correo);
            notificacion.setEstadoEnvio(estadoDeEnvio(correo, notificacion));
            notificacionRepository.save(notificacion);
        } catch (Exception e) {
            log.error("No se pudo procesar la notificacion {}: {}", tipoEvento, e.getMessage());
        }
    }

    private String estadoDeEnvio(String correo, Notificacion notificacion) {
        if (!emailService.estaConfigurado()) {
            return Notificacion.SMTP_NO_CONFIGURADO;
        }
        boolean enviado = emailService.enviar(correo, notificacion.getAsunto(), notificacion.getContenido());
        return enviado ? Notificacion.ENVIADO : Notificacion.FALLIDO;
    }

    // FA01: hasta 3 reintentos, cada 5 minutos; despues queda para revision administrativa.
    @Scheduled(fixedDelay = 300_000, initialDelay = 300_000)
    public void reintentarFallidas() {
        List<Notificacion> pendientes = notificacionRepository
                .findByEstadoEnvioAndReintentosLessThan(Notificacion.FALLIDO, MAX_REINTENTOS);

        for (Notificacion notificacion : pendientes) {
            try {
                boolean enviado = emailService.enviar(notificacion.getCorreoDestino(), notificacion.getAsunto(), notificacion.getContenido());
                notificacion.setReintentos(notificacion.getReintentos() + 1);
                if (enviado) {
                    notificacion.setEstadoEnvio(Notificacion.ENVIADO);
                } else if (notificacion.getReintentos() >= MAX_REINTENTOS) {
                    notificacion.setEstadoEnvio(Notificacion.REVISION_ADMINISTRATIVA);
                }
                notificacionRepository.save(notificacion);
            } catch (Exception e) {
                log.error("Error en reintento de notificacion {}: {}", notificacion.getIdNotificacion(), e.getMessage());
            }
        }
    }

    private String recortar(String texto, int max) {
        if (texto == null) {
            return "";
        }
        return texto.length() <= max ? texto : texto.substring(0, max);
    }
}
