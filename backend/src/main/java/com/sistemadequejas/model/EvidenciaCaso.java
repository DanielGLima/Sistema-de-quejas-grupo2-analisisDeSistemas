package com.sistemadequejas.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "evidencia_caso")
public class EvidenciaCaso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_evidencia")
    private Integer idEvidencia;

    @ManyToOne
    @JoinColumn(name = "id_caso", nullable = false)
    private Caso caso;

    @Column(name = "url_archivo", length = 500, nullable = false)
    private String urlArchivo;

    @Column(name = "tipo_archivo", length = 10, nullable = false)
    private String tipoArchivo;

    @Column(name = "tamano_bytes", nullable = false)
    private Integer tamanoBytes;

    @Column(name = "fecha_carga", nullable = false)
    private LocalDateTime fechaCarga = LocalDateTime.now();

    public EvidenciaCaso() {
    }

    public Integer getIdEvidencia() {
        return idEvidencia;
    }

    public void setIdEvidencia(Integer idEvidencia) {
        this.idEvidencia = idEvidencia;
    }

    public Caso getCaso() {
        return caso;
    }

    public void setCaso(Caso caso) {
        this.caso = caso;
    }

    public String getUrlArchivo() {
        return urlArchivo;
    }

    public void setUrlArchivo(String urlArchivo) {
        this.urlArchivo = urlArchivo;
    }

    public String getTipoArchivo() {
        return tipoArchivo;
    }

    public void setTipoArchivo(String tipoArchivo) {
        this.tipoArchivo = tipoArchivo;
    }

    public Integer getTamanoBytes() {
        return tamanoBytes;
    }

    public void setTamanoBytes(Integer tamanoBytes) {
        this.tamanoBytes = tamanoBytes;
    }

    public LocalDateTime getFechaCarga() {
        return fechaCarga;
    }

    public void setFechaCarga(LocalDateTime fechaCarga) {
        this.fechaCarga = fechaCarga;
    }
}
