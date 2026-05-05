package com.example.demo.dto;

/**
 * Clase enviada y/o recibida del front para mostrar o recibir datos 
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

    // Getters y Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
}