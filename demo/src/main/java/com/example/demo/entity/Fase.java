package com.example.demo.entity;


import jakarta.persistence.*;

/**
 * Entidad que representa las fases de la base de datos
 */

@Entity
@Table(name = "fase", schema = "public")
public class Fase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_fase")
    private Integer id;

    @Column(name = "nombre", nullable = false, length = 45)
    private String nombre;

    @Column(name = "fase_padre")
    private Integer fasePadre;

    public Fase() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Integer getFasePadre() {
        return fasePadre;
    }

    public void setFasePadre(Integer fasePadre) {
        this.fasePadre = fasePadre;
    }
}
