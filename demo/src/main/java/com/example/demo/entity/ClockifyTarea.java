package com.example.demo.entity;

/**
 * Entidad que representa las tareas de Clockify
 */

public class ClockifyTarea {
    private String id;
    private String name;
    private String description;
    private String projectId;
    private String horasTrabajadas;
    
    public ClockifyTarea() {
    }

    public ClockifyTarea(String id, String name, String description, String projectId, String horasTrabajadas) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.projectId = projectId;
        this.horasTrabajadas = horasTrabajadas;
    }

    public String getHorasTrabajadas() {
        return horasTrabajadas;
    }

    public void setHorasTrabajadas(String horasTrabajadas) {
        this.horasTrabajadas = horasTrabajadas;
    }
    
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }
}
