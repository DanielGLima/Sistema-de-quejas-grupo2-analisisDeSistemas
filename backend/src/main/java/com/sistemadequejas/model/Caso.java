package com.sistemadequejas.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "caso")
public class Caso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_caso")
    private Integer idCaso;

    @Column(name = "identificador_visible", length = 20, nullable = false, unique = true)
    private String identificadorVisible;

    @ManyToOne
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @ManyToOne
    @JoinColumn(name = "id_sucursal", nullable = false)
    private Sucursal sucursal;

    @ManyToOne
    @JoinColumn(name = "id_tipo_caso", nullable = false)
    private TipoCaso tipoCaso;

    @ManyToOne
    @JoinColumn(name = "id_categoria")
    private CategoriaCaso categoriaCaso;

    @ManyToOne
    @JoinColumn(name = "id_estado", nullable = false)
    private EstadoCaso estadoCaso;

    @Column(name = "descripcion", length = 1000, nullable = false)
    private String descripcion;

    @Column(name = "numero_factura", length = 20)
    private String numeroFactura;

    @Column(name = "nombre_empleado_involucrado", length = 100)
    private String nombreEmpleadoInvolucrado;

    @Column(name = "es_anonimo", nullable = false)
    private Boolean esAnonimo = false;

    // Se deja como columna simple (sin relacion @ManyToOne a Personal) porque
    // la asignacion de personal (CU-10) todavia no se implementa en esta fase.
    @Column(name = "id_personal_asignado")
    private Integer idPersonalAsignado;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion = LocalDateTime.now();

    public Caso() {
    }

    public Integer getIdCaso() {
        return idCaso;
    }

    public void setIdCaso(Integer idCaso) {
        this.idCaso = idCaso;
    }

    public String getIdentificadorVisible() {
        return identificadorVisible;
    }

    public void setIdentificadorVisible(String identificadorVisible) {
        this.identificadorVisible = identificadorVisible;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public Sucursal getSucursal() {
        return sucursal;
    }

    public void setSucursal(Sucursal sucursal) {
        this.sucursal = sucursal;
    }

    public TipoCaso getTipoCaso() {
        return tipoCaso;
    }

    public void setTipoCaso(TipoCaso tipoCaso) {
        this.tipoCaso = tipoCaso;
    }

    public CategoriaCaso getCategoriaCaso() {
        return categoriaCaso;
    }

    public void setCategoriaCaso(CategoriaCaso categoriaCaso) {
        this.categoriaCaso = categoriaCaso;
    }

    public EstadoCaso getEstadoCaso() {
        return estadoCaso;
    }

    public void setEstadoCaso(EstadoCaso estadoCaso) {
        this.estadoCaso = estadoCaso;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getNumeroFactura() {
        return numeroFactura;
    }

    public void setNumeroFactura(String numeroFactura) {
        this.numeroFactura = numeroFactura;
    }

    public String getNombreEmpleadoInvolucrado() {
        return nombreEmpleadoInvolucrado;
    }

    public void setNombreEmpleadoInvolucrado(String nombreEmpleadoInvolucrado) {
        this.nombreEmpleadoInvolucrado = nombreEmpleadoInvolucrado;
    }

    public Boolean getEsAnonimo() {
        return esAnonimo;
    }

    public void setEsAnonimo(Boolean esAnonimo) {
        this.esAnonimo = esAnonimo;
    }

    public Integer getIdPersonalAsignado() {
        return idPersonalAsignado;
    }

    public void setIdPersonalAsignado(Integer idPersonalAsignado) {
        this.idPersonalAsignado = idPersonalAsignado;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }

    public void setFechaActualizacion(LocalDateTime fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }
}
