package com.sistemadequejas.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// CU-14: registro del resultado de cada notificacion automatica.
@Entity
@Table(name = "notificacion")
public class Notificacion {

    public static final String PENDIENTE = "Pendiente";
    public static final String ENVIADO = "Enviado";
    public static final String FALLIDO = "Fallido";
    public static final String REVISION_ADMINISTRATIVA = "Revisión administrativa";
    public static final String SMTP_NO_CONFIGURADO = "Omitido (SMTP no configurado)";
    public static final String SIN_CONTACTO = "Sin contacto válido";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_notificacion")
    private Integer idNotificacion;

    @ManyToOne
    @JoinColumn(name = "id_caso")
    private Caso caso;

    @ManyToOne
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @ManyToOne
    @JoinColumn(name = "id_personal")
    private Personal personal;

    @Column(name = "correo_destino", length = 100, nullable = false)
    private String correoDestino;

    @Column(name = "tipo_evento", length = 50, nullable = false)
    private String tipoEvento;

    @Column(name = "asunto", length = 100, nullable = false)
    private String asunto;

    @Column(name = "contenido", length = 500, nullable = false)
    private String contenido;

    @Column(name = "estado_envio", length = 30, nullable = false)
    private String estadoEnvio = PENDIENTE;

    @Column(name = "reintentos", nullable = false)
    private Integer reintentos = 0;

    @Column(name = "fecha_envio", nullable = false)
    private LocalDateTime fechaEnvio = LocalDateTime.now();

    public Notificacion() {
    }

    public Integer getIdNotificacion() {
        return idNotificacion;
    }

    public void setIdNotificacion(Integer idNotificacion) {
        this.idNotificacion = idNotificacion;
    }

    public Caso getCaso() {
        return caso;
    }

    public void setCaso(Caso caso) {
        this.caso = caso;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public Personal getPersonal() {
        return personal;
    }

    public void setPersonal(Personal personal) {
        this.personal = personal;
    }

    public String getCorreoDestino() {
        return correoDestino;
    }

    public void setCorreoDestino(String correoDestino) {
        this.correoDestino = correoDestino;
    }

    public String getTipoEvento() {
        return tipoEvento;
    }

    public void setTipoEvento(String tipoEvento) {
        this.tipoEvento = tipoEvento;
    }

    public String getAsunto() {
        return asunto;
    }

    public void setAsunto(String asunto) {
        this.asunto = asunto;
    }

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }

    public String getEstadoEnvio() {
        return estadoEnvio;
    }

    public void setEstadoEnvio(String estadoEnvio) {
        this.estadoEnvio = estadoEnvio;
    }

    public Integer getReintentos() {
        return reintentos;
    }

    public void setReintentos(Integer reintentos) {
        this.reintentos = reintentos;
    }

    public LocalDateTime getFechaEnvio() {
        return fechaEnvio;
    }

    public void setFechaEnvio(LocalDateTime fechaEnvio) {
        this.fechaEnvio = fechaEnvio;
    }
}
