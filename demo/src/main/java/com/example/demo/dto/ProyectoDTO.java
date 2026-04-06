package com.example.demo.dto;

public class ProyectoDTO {
    private Long id;
    private String nombre;
    private String descripcion;
    private String fechaInicio;
    private boolean activo;

    //No se enseña pero se guarda
    private String gitlabId;
    private String clockifyId;
    private boolean enBaseDatos; // Para saber si existe en Neon

    public ProyectoDTO() {
    }

    public ProyectoDTO(Long id, String nombre, String descripcion, String fechaInicio, boolean activo, String gitlabId, String clockifyId, boolean enBaseDatos) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.fechaInicio = fechaInicio;
        this.activo = activo;
        this.gitlabId = gitlabId;
        this.clockifyId = clockifyId;
        this.enBaseDatos = enBaseDatos;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public String getFechaInicio() { return fechaInicio; }
    public boolean isActivo() { return activo; }
    public String getGitlabId() { return gitlabId; }
    public String getClockifyId() { return clockifyId; }
    public boolean isEnBaseDatos() { return enBaseDatos; }
    
    public void setId(Long id) { this.id = id; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public void setFechaInicio(String fechaInicio) { this.fechaInicio = fechaInicio; }
    public void setActivo(boolean activo) { this.activo = activo; }
    public void setGitlabId(String gitlabId) { this.gitlabId = gitlabId; }
    public void setClockifyId(String clockifyId) { this.clockifyId = clockifyId; }
    public void setEnBaseDatos(boolean enBaseDatos) { this.enBaseDatos = enBaseDatos; }
}
