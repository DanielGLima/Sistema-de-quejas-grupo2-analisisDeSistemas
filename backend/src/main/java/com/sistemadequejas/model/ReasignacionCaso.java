package com.sistemadequejas.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDateTime;

// CU-10: historial de cambios de responsable de un caso.
@Entity
@Table(name = "reasignacion_caso")
public class ReasignacionCaso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_reasignacion")
    private Integer idReasignacion;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "id_caso", nullable = false)
    private Caso caso;

    @ManyToOne
    @JoinColumn(name = "id_personal_anterior")
    private Personal personalAnterior;

    @ManyToOne
    @JoinColumn(name = "id_personal_nuevo", nullable = false)
    private Personal personalNuevo;

    @ManyToOne
    @JoinColumn(name = "id_personal_ejecutor", nullable = false)
    private Personal personalEjecutor;

    @Column(name = "motivo", length = 300, nullable = false)
    private String motivo;

    @Column(name = "fecha_reasignacion", nullable = false)
    private LocalDateTime fechaReasignacion = LocalDateTime.now();

    public ReasignacionCaso() {
    }

    public Integer getIdReasignacion() {
        return idReasignacion;
    }

    public void setIdReasignacion(Integer idReasignacion) {
        this.idReasignacion = idReasignacion;
    }

    public Caso getCaso() {
        return caso;
    }

    public void setCaso(Caso caso) {
        this.caso = caso;
    }

    public Personal getPersonalAnterior() {
        return personalAnterior;
    }

    public void setPersonalAnterior(Personal personalAnterior) {
        this.personalAnterior = personalAnterior;
    }

    public Personal getPersonalNuevo() {
        return personalNuevo;
    }

    public void setPersonalNuevo(Personal personalNuevo) {
        this.personalNuevo = personalNuevo;
    }

    public Personal getPersonalEjecutor() {
        return personalEjecutor;
    }

    public void setPersonalEjecutor(Personal personalEjecutor) {
        this.personalEjecutor = personalEjecutor;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public LocalDateTime getFechaReasignacion() {
        return fechaReasignacion;
    }

    public void setFechaReasignacion(LocalDateTime fechaReasignacion) {
        this.fechaReasignacion = fechaReasignacion;
    }
}
