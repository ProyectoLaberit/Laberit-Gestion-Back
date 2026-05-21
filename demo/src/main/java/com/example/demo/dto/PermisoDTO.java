package com.example.demo.dto;
/**
 * Clase que contiene informacion de los permisos de un rol
 */
public class PermisoDTO {
    private Integer id;
    private String nombre;

    public PermisoDTO() {
    }

    public PermisoDTO(Integer id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
}