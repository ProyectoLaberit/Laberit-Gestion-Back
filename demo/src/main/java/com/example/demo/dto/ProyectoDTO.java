package com.example.demo.dto;

public class ProyectoDTO {
    private String nombre;
    private String descripcion;
    private String fechaInicio;
    private boolean activo;

    public ProyectoDTO() {
    }

    public ProyectoDTO(String nombre, String descripcion, String fechaInicio, boolean activo) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.fechaInicio = fechaInicio;
        this.activo = activo;
    }

    // Getters and Setters
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public String getFechaInicio() { return fechaInicio; }
    public boolean isActivo() { return activo; }
}
