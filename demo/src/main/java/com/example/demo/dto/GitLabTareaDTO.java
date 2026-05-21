package com.example.demo.dto;

import java.util.List;
/**
 *  DTO (Data Transfer Object) para representar una tarea de GitLab en las respuestas de la API.
*/
/**
 * Clase para extraer las tareas de GitLab de la API de GitLab y adaptarla a nuestro sistema de datos
 */
public class GitLabTareaDTO {
    private String id;
    private Long iid;
    private String title;
    private List<String> labels;
    private String estado;

    public GitLabTareaDTO() {
    }

    public GitLabTareaDTO(String id, Long iid, String title, List<String> labels, String estado) {
        this.id = id;
        this.iid = iid;
        this.title = title;
        this.labels = labels;
        this.estado = estado;
        
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Long getIid() { return iid; }
    public void setIid(Long iid) { this.iid = iid; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public List<String> getLabels() { return labels; }
    public void setLabels(List<String> labels) { this.labels = labels; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}