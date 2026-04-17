package com.example.demo.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

public class ProyectoDTO {
    private Long id;
    private String nombre;
    private String descripcion;

    private Boolean excels;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fechaInicio;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fechaFin;

    private Boolean activo;


    public ProyectoDTO() {
    }

    public ProyectoDTO(Long id, String nombre, String descripcion, LocalDate fechaInicio, LocalDate fechaFin, Boolean activo, Boolean excels) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.activo = activo;
        this.excels = excels;
    }

    public ProyectoDTO(String nombre, String descripcion, LocalDate fechaInicio, LocalDate fechaFin, Boolean activo) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.activo = activo;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public LocalDate getFechaInicio() { return fechaInicio; }
    public LocalDate getFechaFin() { return fechaFin; }
    public Boolean isActivo() { return activo; }
    public Boolean getExcels() { return excels; }
    
    public void setId(Long id) { this.id = id; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }
    public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }
    public void setActivo(Boolean activo) { this.activo = activo; }
    public void setExcels(Boolean excels) { this.excels = excels; }
}
