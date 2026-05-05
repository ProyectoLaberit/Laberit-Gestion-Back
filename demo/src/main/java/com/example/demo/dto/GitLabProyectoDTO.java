package com.example.demo.dto;

// DTO para representar un proyecto de GitLab que aún no ha sido registrado en nuestra base de datos

/**
 * Clase enviada y/o recibida del front para mostrar o recibir datos de los proyectos de gitlab
 */
public class GitLabProyectoDTO {
    private String id;
    private String nombre;

    public GitLabProyectoDTO(String id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    public String getId() { return id; }
    public String getNombre() { return nombre; }

    public void setId(String id) { this.id = id; }
    public void setNombre(String nombre) { this.nombre = nombre; }
}
