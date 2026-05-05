package com.example.demo.entity;

import jakarta.persistence.*;

/* Entidad que representa un departamento en el sistema.
    * Un departamento puede tener un departamento padre, lo que permite crear una jerarquía.
    * La tabla en la base de datos tendrá una relación recursiva consigo misma a través de la columna "departamento_padre".
*/

@Entity
@Table(name = "departamento")
public class Departamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_departamento") // Asegúrate de que este es el nombre en tu DB
    private int id;

    private String nombre;

    // Relación recursiva con la misma tabla
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "departamento_padre") // Asegúrate de que este es el nombre en tu DB
    private Departamento padre;

    public Departamento() {
    }

    public Departamento(int id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    // --- GETTERS Y SETTERS ---
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