package com.example.demo.dto;

import java.util.List;

/**
 * Objeto de transferencia de datos para representar una tarea (Issue/Work Item)
 * de GitLab.
 * Este DTO se utiliza para simplificar la respuesta de la API externa y
 * adaptarla a las necesidades de nuestro sistema de gestión de tiempos.
 */
public class GitLabTareaDTO {
    private String id; // ID global de GitLab (ej: 12345678)
    private Long numeroGitLab; // ID interno del proyecto (el que ve el usuario, ej: #42)
    private String title; // Título de la tarea
    private List<String> labels; // Etiquetas asociadas a la tarea
    private String estado; // Estado de la tarea (ej: "opened", "closed")
    private boolean vinculada; // true = Nombre bien y guardada en DB (o corregida). false = Nombre mal y fuera
                               // de la DB.

    public GitLabTareaDTO() {
    }

    // Constructor para mapear fácilmente desde el Map de GitLab
    public GitLabTareaDTO(String id, Long numeroGitLab, String title, List<String> labels, String estado) {
        this.id = id;
        this.numeroGitLab = numeroGitLab;
        this.title = title;
        this.labels = labels;
        this.estado = estado;
        this.vinculada = false; // Por defecto nace en false hasta que el servicio verifique la DB
    }

    // Getters y Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    // 🛠️ Corregido a CamelCase (getNumeroGitLab) para evitar errores de mapeo en
    // Spring/Jackson
    public Long getNumeroGitLab() {
        return numeroGitLab;
    }

    public void setNumeroGitLab(Long numeroGitLab) {
        this.numeroGitLab = numeroGitLab;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<String> getLabels() {
        return labels;
    }

    public void setLabels(List<String> labels) {
        this.labels = labels;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public boolean isVinculada() {
        return vinculada;
    }

    public void setVinculada(boolean vinculada) {
        this.vinculada = vinculada;
    }
}