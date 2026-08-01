package com.sistemadequejas.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tipo_queja")
public class TipoQueja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tipo_queja")
    private Integer idTipoQueja;

    @Column(name = "nombre", length = 50, nullable = false, unique = true)
    private String nombre;

    @Column(name = "prefijo", length = 5, nullable = false, unique = true)
    private String prefijo;

    public TipoQueja() {
    }

    public Integer getIdTipoQueja() {
        return idTipoQueja;
    }

    public void setIdTipoQueja(Integer idTipoQueja) {
        this.idTipoQueja = idTipoQueja;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getPrefijo() {
        return prefijo;
    }

    public void setPrefijo(String prefijo) {
        this.prefijo = prefijo;
    }
}
