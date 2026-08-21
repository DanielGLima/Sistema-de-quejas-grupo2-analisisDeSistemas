package com.sistemadequejas.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "queja")
public class Queja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_queja")
    private Integer idQueja;

    @ManyToOne
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @ManyToOne
    @JoinColumn(name = "id_tipo_queja", nullable = false)
    private TipoQueja tipoQueja;

    @ManyToOne
    @JoinColumn(name = "id_estado", nullable = false)
    private Estado estado;

    @Column(name = "numero_factura", length = 30)
    private String numeroFactura;

    @Column(name = "descripcion", nullable = false)
    private String descripcion;

    @Column(name = "fecha_hora_incidente", nullable = false)
    private LocalDateTime fechaHoraIncidente;

    @Column(name = "empleado_atendio", length = 150)
    private String empleadoAtendio;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    public Queja() {
    }

    public Integer getIdQueja() {
        return idQueja;
    }

    public void setIdQueja(Integer idQueja) {
        this.idQueja = idQueja;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public TipoQueja getTipoQueja() {
        return tipoQueja;
    }

    public void setTipoQueja(TipoQueja tipoQueja) {
        this.tipoQueja = tipoQueja;
    }

    public Estado getEstado() {
        return estado;
    }

    public void setEstado(Estado estado) {
        this.estado = estado;
    }

    public String getNumeroFactura() {
        return numeroFactura;
    }

    public void setNumeroFactura(String numeroFactura) {
        this.numeroFactura = numeroFactura;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public LocalDateTime getFechaHoraIncidente() {
        return fechaHoraIncidente;
    }

    public void setFechaHoraIncidente(LocalDateTime fechaHoraIncidente) {
        this.fechaHoraIncidente = fechaHoraIncidente;
    }

    public String getEmpleadoAtendio() {
        return empleadoAtendio;
    }

    public void setEmpleadoAtendio(String empleadoAtendio) {
        this.empleadoAtendio = empleadoAtendio;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }
}
