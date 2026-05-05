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
    private Long iid;
    private String title;
    private List<String> labels;

    public GitLabTareaDTO() {
    }

    public GitLabTareaDTO(Object id, Object iid, Object title, Object labels) {
        this.id = String.valueOf(id);
        this.iid = Long.valueOf(String.valueOf(iid));
        this.title = String.valueOf(title);
        this.labels = (List<String>) labels;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getIid() {
        return iid;
    }

    public void setIid(Long iid) {
        this.iid = iid;
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
}