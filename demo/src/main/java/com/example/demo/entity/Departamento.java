package com.example.demo.entity;

import jakarta.persistence.*;
/**
 * Entidad que representa los departamentos de la base de datos
 * Un departamento puede tener un departamento padre, lo que permite crear una jerarquía
 * La tabla de la base de datos tiene una relacion recursiva consigo misma a traves de la columna "departamento_padre"
 */

@Entity
@Table(name = "departamento")
public class Departamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_departamento")
    private int id;

    private String nombre;

    // Relación recursiva con la misma tabla
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "departamento_padre")
    private Departamento padre;

    public Departamento() {
    }

    public Departamento(int id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Departamento getPadre() {
        return padre;
    }

    public void setPadre(Departamento padre) {
        this.padre = padre;
    }
}