package com.sistemadequejas.model;

import jakarta.persistence.*;

@Entity
@Table(name = "tipo_caso")
public class TipoCaso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tipo_caso")
    private Integer idTipoCaso;

    @Column(name = "codigo", length = 3, nullable = false, unique = true)
    private String codigo;

    @Column(name = "nombre", length = 20, nullable = false, unique = true)
    private String nombre;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    public TipoCaso() {
    }

    public Integer getIdTipoCaso() {
        return idTipoCaso;
    }

    public void setIdTipoCaso(Integer idTipoCaso) {
        this.idTipoCaso = idTipoCaso;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }
}
