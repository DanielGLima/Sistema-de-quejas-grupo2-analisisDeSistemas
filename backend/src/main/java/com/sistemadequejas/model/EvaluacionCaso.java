package com.sistemadequejas.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "evaluacion_caso")
public class EvaluacionCaso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_evaluacion")
    private Integer idEvaluacion;

    @JsonIgnore
    @OneToOne
    @JoinColumn(name = "id_caso", nullable = false, unique = true)
    private Caso caso;

    @Column(name = "calificacion", nullable = false)
    private Short calificacion;

    @Column(name = "comentario", length = 500)
    private String comentario;

    @Column(name = "fecha_evaluacion", nullable = false)
    private LocalDateTime fechaEvaluacion = LocalDateTime.now();

    public EvaluacionCaso() {
    }

    public Integer getIdEvaluacion() {
        return idEvaluacion;
    }

    public void setIdEvaluacion(Integer idEvaluacion) {
        this.idEvaluacion = idEvaluacion;
    }

    public Caso getCaso() {
        return caso;
    }

    public void setCaso(Caso caso) {
        this.caso = caso;
    }

    public Short getCalificacion() {
        return calificacion;
    }

    public void setCalificacion(Short calificacion) {
        this.calificacion = calificacion;
    }

    public String getComentario() {
        return comentario;
    }

    public void setComentario(String comentario) {
        this.comentario = comentario;
    }

    public LocalDateTime getFechaEvaluacion() {
        return fechaEvaluacion;
    }

    public void setFechaEvaluacion(LocalDateTime fechaEvaluacion) {
        this.fechaEvaluacion = fechaEvaluacion;
    }
}
