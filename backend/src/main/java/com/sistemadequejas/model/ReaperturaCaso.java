package com.sistemadequejas.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDateTime;

// CU-09: solicitud de reapertura de un caso cerrado.
@Entity
@Table(name = "reapertura_caso")
public class ReaperturaCaso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_reapertura")
    private Integer idReapertura;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "id_caso", nullable = false)
    private Caso caso;

    @Column(name = "motivo", length = 500, nullable = false)
    private String motivo;

    @Column(name = "url_evidencia", length = 500)
    private String urlEvidencia;

    @Column(name = "fecha_solicitud", nullable = false)
    private LocalDateTime fechaSolicitud = LocalDateTime.now();

    public ReaperturaCaso() {
    }

    public Integer getIdReapertura() {
        return idReapertura;
    }

    public void setIdReapertura(Integer idReapertura) {
        this.idReapertura = idReapertura;
    }

    public Caso getCaso() {
        return caso;
    }

    public void setCaso(Caso caso) {
        this.caso = caso;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getUrlEvidencia() {
        return urlEvidencia;
    }

    public void setUrlEvidencia(String urlEvidencia) {
        this.urlEvidencia = urlEvidencia;
    }

    public LocalDateTime getFechaSolicitud() {
        return fechaSolicitud;
    }

    public void setFechaSolicitud(LocalDateTime fechaSolicitud) {
        this.fechaSolicitud = fechaSolicitud;
    }
}
