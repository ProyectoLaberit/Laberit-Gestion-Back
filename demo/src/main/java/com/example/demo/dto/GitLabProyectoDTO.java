package com.example.demo.dto;

/**
 *  DTO (Data Transfer Object) para representar un proyecto de GitLab en las respuestas de la API.
*/
/**
 * Clase que contiene los datos de los proyectos de gitlab que aun no ha sido registrado en base de datos
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
