package com.example.demo.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
/**
 *  DTO (Data Transfer Object) para representar un proyecto en las respuestas de la API.
*/
/**
 * Clase enviada y/o recibida del front con la informacion de un proyecto de la base de datos
 */
public class ProyectoDTO {
    private Long id;
    private String nombre;
    private String descripcion;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String clockifyId;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String gitlabId;

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

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public String getClockifyId() { return clockifyId; }
    public String getGitlabId() { return gitlabId; }
    public LocalDate getFechaInicio() { return fechaInicio; }
    public LocalDate getFechaFin() { return fechaFin; }
    public Boolean isActivo() { return activo; }
    public Boolean getExcels() { return excels; }
    
    
    public void setId(Long id) { this.id = id; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public void setClockifyId(String clockifyId) { this.clockifyId = clockifyId; }
    public void setGitlabId(String gitlabId) { this.gitlabId = gitlabId; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }
    public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }
    public void setActivo(Boolean activo) { this.activo = activo; }
    public void setExcels(Boolean excels) { this.excels = excels; }
}
