package com.example.demo.dto;

import java.util.List;

/**
 * Clase enviada y/o recibida del front para mostrar o recibir datos 
 */
public class RolDTO {
    private Integer id;
    private String nombre;
    private List<PermisoDTO> permisos;

    public RolDTO() {
    }

    public RolDTO(Integer id, String nombre, List<PermisoDTO> permisos) {
        this.id = id;
        this.nombre = nombre;
        this.permisos = permisos;
    }

    // Getters y Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public List<PermisoDTO> getPermisos() { return permisos; }
    public void setPermisos(List<PermisoDTO> permisos) { this.permisos = permisos; }
}