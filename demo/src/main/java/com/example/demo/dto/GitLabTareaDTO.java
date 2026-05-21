package com.example.demo.dto;

import java.util.List;
/**
 *  DTO (Data Transfer Object) para representar una tarea de GitLab en las respuestas de la API.
*/
/**
 * Clase para extraer las tareas de GitLab de la API de GitLab y adaptarla a nuestro sistema de datos
 */
public class GitLabTareaDTO {
    private String id;     // ID global de GitLab (ej: 12345678)
    private Long numeroGitLab;      // ID interno del proyecto (el que ve el usuario, ej: #42)
    private String title;  // Título de la tarea
    private List<String> labels; // Etiquetas asociadas a la tarea
    private String estado; // Estado de la tarea (ej: "opened", "closed", "in_progress")

    public GitLabTareaDTO() {
    }

    // Constructor para mapear fácilmente desde el Map de GitLab
    public GitLabTareaDTO(String id, Long numeroGitLab, String title, List<String> labels, String estado) {
        this.id = id;
        this.numeroGitLab = numeroGitLab;
        this.title = title;
        this.labels = labels;
        this.estado = estado;
        
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Long getnumeroGitLab() { return numeroGitLab; }
    public void setnumeroGitLab(Long numeroGitLab) { this.numeroGitLab = numeroGitLab; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public List<String> getLabels() { return labels; }
    public void setLabels(List<String> labels) { this.labels = labels; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}