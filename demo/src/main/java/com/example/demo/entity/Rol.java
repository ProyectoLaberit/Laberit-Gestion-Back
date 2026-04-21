package com.example.demo.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rol")
public class Rol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_rol")
    private Integer id;

    private String nombre;

    @ManyToMany
    @JoinTable(
        name = "rol_x_permiso",
        joinColumns = @JoinColumn(name = "id_rol"), // La columna que apunta a la clase rol
        inverseJoinColumns = @JoinColumn(name = "id_permiso") // La columna que apunta a la clase permiso
    )
    private List<Permiso> permisos = new ArrayList<>();

    public Rol() {
    }

    // --- GETTERS Y SETTERS ---

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public List<Permiso> getPermisos() { return permisos; }
    public void setPermisos(List<Permiso> permisos) { this.permisos = permisos; }
}