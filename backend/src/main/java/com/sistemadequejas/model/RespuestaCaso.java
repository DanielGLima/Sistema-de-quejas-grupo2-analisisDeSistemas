package com.sistemadequejas.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDateTime;

// CU-11: respuesta oficial asociada a un caso.
@Entity
@Table(name = "respuesta_caso")
public class RespuestaCaso {

    public static final String APROBADA = "Aprobada";
    public static final String PENDIENTE_APROBACION = "Pendiente de aprobación";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_respuesta")
    private Integer idRespuesta;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "id_caso", nullable = false)
    private Caso caso;

    @ManyToOne
    @JoinColumn(name = "id_personal", nullable = false)
    private Personal personal;

    @Column(name = "titulo", length = 100, nullable = false)
    private String titulo;

    @Column(name = "contenido", length = 1000, nullable = false)
    private String contenido;

    @Column(name = "acciones_seguimiento", length = 300)
    private String accionesSeguimiento;

    @Column(name = "estado_aprobacion", length = 30, nullable = false)
    private String estadoAprobacion = APROBADA;

    @Column(name = "fecha_respuesta", nullable = false)
    private LocalDateTime fechaRespuesta = LocalDateTime.now();

    public RespuestaCaso() {
    }

    public Integer getIdRespuesta() {
        return idRespuesta;
    }

    public void setIdRespuesta(Integer idRespuesta) {
        this.idRespuesta = idRespuesta;
    }

    public Caso getCaso() {
        return caso;
    }

    public void setCaso(Caso caso) {
        this.caso = caso;
    }

    public Personal getPersonal() {
        return personal;
    }

    public void setPersonal(Personal personal) {
        this.personal = personal;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }

    public String getAccionesSeguimiento() {
        return accionesSeguimiento;
    }

    public void setAccionesSeguimiento(String accionesSeguimiento) {
        this.accionesSeguimiento = accionesSeguimiento;
    }

    public String getEstadoAprobacion() {
        return estadoAprobacion;
    }

    public void setEstadoAprobacion(String estadoAprobacion) {
        this.estadoAprobacion = estadoAprobacion;
    }

    public LocalDateTime getFechaRespuesta() {
        return fechaRespuesta;
    }

    public void setFechaRespuesta(LocalDateTime fechaRespuesta) {
        this.fechaRespuesta = fechaRespuesta;
    }
}
