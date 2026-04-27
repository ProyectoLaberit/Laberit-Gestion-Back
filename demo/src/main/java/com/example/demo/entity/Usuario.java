package com.example.demo.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;

@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Integer id;

    @Column(name = "nombre")
    private String nombre;
    
    @Column(name = "email")
    private String email;

    @Column(name = "contraseña")
    private String password;

    @Column(name = "foto")
    private String foto;

    @Column(name = "excels")
    private Boolean excels;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "usuario_x_rol", // El nombre de la tabla intermedia
        joinColumns = @JoinColumn(name = "id_usuario"), // La columna que apunta al usuario
        inverseJoinColumns = @JoinColumn(name = "id_rol") // La columna que apunta al rol
    )
    private List<Rol> roles = new ArrayList<>();

    public Usuario() {
    }

    // --- GETTERS Y SETTERS ---

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFoto() { return foto; }
    public void setFoto(String foto) { this.foto = foto; }

    public Boolean getExcels() { return excels; }
    public void setExcels(Boolean excels) { this.excels = excels; }

    public List<Rol> getRoles() { return roles; }
    public void setRoles(List<Rol> roles) { this.roles = roles; }
}