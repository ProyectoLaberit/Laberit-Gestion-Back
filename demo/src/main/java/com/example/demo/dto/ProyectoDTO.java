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
    
    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public void setFechaInicio(String fechaInicio) { this.fechaInicio = fechaInicio; }
    public void setActivo(boolean activo) { this.activo = activo; }
}
