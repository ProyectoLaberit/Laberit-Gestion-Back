package com.example.demo.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

/**
 * Entidad que representa las tareas de gitlab de la base de datos
 */
@Entity
@Table(name = "tarea_gitlab")
public class GitLabTarea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "issue_id", nullable = false, length = 100)
    private String issueId;

    @Column(name = "numero_gitlab", nullable = false)
    private Long numeroGitlab;

    @Column(name = "titulo", nullable = false)
    private String titulo;

    @Column(name = "estado", nullable = false, length = 50)
    private String estado;

    @Column(name = "valida", nullable = false)
    private Boolean valida = false;

    @Column(name = "id_tarea_proyecto")
    private Long tareaProyecto;

    @Column(name = "id_proyecto")
    private Long idProyecto;


    
    public GitLabTarea() {
    }

    
    public GitLabTarea(Long id, String issueId, Long numeroGitLab, String titulo, String estado, Boolean valida,
            Long idProyecto, Long tareaProyecto) {
        this.id = id;
        this.issueId = issueId;
        this.numeroGitlab = numeroGitlab;
        this.titulo = titulo;
        this.estado = estado;
        this.valida = valida;
        this.tareaProyecto = tareaProyecto;
        this.idProyecto = idProyecto;
    }

    // --- GETTERS Y SETTERS ---
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIssueId() {
        return issueId;
    }

    public void setIssueId(String issueId) {
        this.issueId = issueId;
    }

    public Long getNumeroGitlab() {
        return numeroGitlab;
    }

    public void setNumeroGitlab(Long numeroGitlab) {
        this.numeroGitlab = numeroGitlab;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Boolean getValida() {
        return valida;
    }

    public void setValida(Boolean valida) {
        this.valida = valida;
    }

    
    public Long getIdProyecto() {
        return idProyecto;
    }

   
    public void setIdProyecto(Long idProyecto) {
        this.idProyecto = idProyecto;
    }

    public Long getTareaProyecto() {
        return tareaProyecto;
    }

    public void setTareaProyecto(Long tareaProyecto) {
        this.tareaProyecto = tareaProyecto;
    }


    
}