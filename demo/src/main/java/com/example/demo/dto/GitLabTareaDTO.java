package com.example.demo.dto;

public class GitLabTareaDTO {
    private String id;     // ID global de GitLab (ej: 12345678)
    private Long iid;      // ID interno del proyecto (el que ve el usuario, ej: #42)
    private String title;  // Título de la tarea

    // Constructor vacío
    public GitLabTareaDTO() {}

    // Constructor para mapear fácilmente desde el Map de GitLab
    public GitLabTareaDTO(Object id, Object iid, Object title) {
        this.id = String.valueOf(id);
        this.iid = Long.valueOf(String.valueOf(iid));
        this.title = String.valueOf(title);
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Long getIid() { return iid; }
    public void setIid(Long iid) { this.iid = iid; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
}