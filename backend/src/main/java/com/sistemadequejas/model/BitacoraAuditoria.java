package com.sistemadequejas.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "bitacora_auditoria")
public class BitacoraAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_bitacora")
    private Integer idBitacora;

    @Column(name = "tipo_actor", length = 10, nullable = false)
    private String tipoActor;

    @ManyToOne
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    // Sin relacion a Personal en esta fase (todavia no se implementa esa entidad).
    @Column(name = "id_personal")
    private Integer idPersonal;

    @ManyToOne
    @JoinColumn(name = "id_caso")
    private Caso caso;

    @Column(name = "accion", length = 50, nullable = false)
    private String accion;

    @Column(name = "modulo_afectado", length = 40)
    private String moduloAfectado;

    @Column(name = "ip_origen", length = 45)
    private String ipOrigen;

    @Column(name = "detalle", length = 1500)
    private String detalle;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora = LocalDateTime.now();

    public BitacoraAuditoria() {
    }

    public Integer getIdBitacora() {
        return idBitacora;
    }

    public void setIdBitacora(Integer idBitacora) {
        this.idBitacora = idBitacora;
    }

    public String getTipoActor() {
        return tipoActor;
    }

    public void setTipoActor(String tipoActor) {
        this.tipoActor = tipoActor;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public Integer getIdPersonal() {
        return idPersonal;
    }

    public void setIdPersonal(Integer idPersonal) {
        this.idPersonal = idPersonal;
    }

    public Caso getCaso() {
        return caso;
    }

    public void setCaso(Caso caso) {
        this.caso = caso;
    }

    public String getAccion() {
        return accion;
    }

    public void setAccion(String accion) {
        this.accion = accion;
    }

    public String getModuloAfectado() {
        return moduloAfectado;
    }

    public void setModuloAfectado(String moduloAfectado) {
        this.moduloAfectado = moduloAfectado;
    }

    public String getIpOrigen() {
        return ipOrigen;
    }

    public void setIpOrigen(String ipOrigen) {
        this.ipOrigen = ipOrigen;
    }

    public String getDetalle() {
        return detalle;
    }

    public void setDetalle(String detalle) {
        this.detalle = detalle;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }
}
