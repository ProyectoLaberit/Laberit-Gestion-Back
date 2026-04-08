package com.example.demo.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

public class ProyectoDTO {
    private Long id;
    private String nombre;
    private String descripcion;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fechaInicio;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fechaFin;

    private Boolean activo;

    //No se enseña pero se guarda
    private String gitlabId;
    private String clockifyId;
    private Boolean enBaseDatos; // Para saber si existe en Neon

    public ProyectoDTO() {
    }

    public ProyectoDTO(Long id, String nombre, String descripcion, LocalDate fechaInicio, LocalDate fechaFin, Boolean activo, String gitlabId, String clockifyId, Boolean enBaseDatos) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.activo = activo;
        this.gitlabId = gitlabId;
        this.clockifyId = clockifyId;
        this.enBaseDatos = enBaseDatos;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public LocalDate getFechaInicio() { return fechaInicio; }
    public LocalDate getFechaFin() { return fechaFin; }
    public Boolean isActivo() { return activo; }
    public String getGitlabId() { return gitlabId; }
    public String getClockifyId() { return clockifyId; }
    public Boolean isEnBaseDatos() { return enBaseDatos; }
    
    public void setId(Long id) { this.id = id; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }
    public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }
    public void setActivo(Boolean activo) { this.activo = activo; }
    public void setGitlabId(String gitlabId) { this.gitlabId = gitlabId; }
    public void setClockifyId(String clockifyId) { this.clockifyId = clockifyId; }
    public void setEnBaseDatos(Boolean enBaseDatos) { this.enBaseDatos = enBaseDatos; }
}
