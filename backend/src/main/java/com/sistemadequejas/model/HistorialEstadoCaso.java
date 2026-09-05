package com.sistemadequejas.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "historial_estado_caso")
public class HistorialEstadoCaso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_historial")
    private Integer idHistorial;

    @ManyToOne
    @JoinColumn(name = "id_caso", nullable = false)
    private Caso caso;

    @ManyToOne
    @JoinColumn(name = "id_estado_anterior")
    private EstadoCaso estadoAnterior;

    @ManyToOne
    @JoinColumn(name = "id_estado_nuevo", nullable = false)
    private EstadoCaso estadoNuevo;

    // Sin relacion a Personal: en esta fase los cambios de estado los origina
    // el propio usuario (CU-07), no personal interno (eso es CU-10).
    @Column(name = "id_personal")
    private Integer idPersonal;

    @Column(name = "observacion", length = 500)
    private String observacion;

    @Column(name = "fecha_cambio", nullable = false)
    private LocalDateTime fechaCambio = LocalDateTime.now();

    public HistorialEstadoCaso() {
    }

    public Integer getIdHistorial() {
        return idHistorial;
    }

    public void setIdHistorial(Integer idHistorial) {
        this.idHistorial = idHistorial;
    }

    public Caso getCaso() {
        return caso;
    }

    public void setCaso(Caso caso) {
        this.caso = caso;
    }

    public EstadoCaso getEstadoAnterior() {
        return estadoAnterior;
    }

    public void setEstadoAnterior(EstadoCaso estadoAnterior) {
        this.estadoAnterior = estadoAnterior;
    }

    public EstadoCaso getEstadoNuevo() {
        return estadoNuevo;
    }

    public void setEstadoNuevo(EstadoCaso estadoNuevo) {
        this.estadoNuevo = estadoNuevo;
    }

    public Integer getIdPersonal() {
        return idPersonal;
    }

    public void setIdPersonal(Integer idPersonal) {
        this.idPersonal = idPersonal;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public LocalDateTime getFechaCambio() {
        return fechaCambio;
    }

    public void setFechaCambio(LocalDateTime fechaCambio) {
        this.fechaCambio = fechaCambio;
    }
}
