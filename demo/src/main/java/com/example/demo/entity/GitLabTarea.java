package com.example.demo.entity;

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

    @Column(name = "numero_git_lab", nullable = false)
    private Long numeroGitLab;

    @Column(name = "titulo", nullable = false)
    private String titulo;

    @Column(name = "estado", nullable = false, length = 50)
    private String estado;


    @Column(name = "valida")
    private Boolean valida = false;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tarea_proyecto", nullable = true)
    private TareaProyecto tareaProyecto;

    // --- CONSTRUCTORES ---
    public GitLabTarea() {
    }

    public GitLabTarea(Long id, String issueId, Long numeroGitLab, String titulo, String estado, Boolean valida,
            TareaProyecto tareaProyecto) {
        this.id = id;
        this.issueId = issueId;
        this.numeroGitLab = numeroGitLab;
        this.titulo = titulo;
        this.estado = estado;
        this.valida = valida;
        this.tareaProyecto = tareaProyecto;
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

    public Long getNumeroGitLab() {
        return numeroGitLab;
    }

    public void setNumeroGitLab(Long numeroGitLab) {
        this.numeroGitLab = numeroGitLab;
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

    public TareaProyecto getTareaProyecto() {
        return tareaProyecto;
    }

    public void setTareaProyecto(TareaProyecto tareaProyecto) {
        this.tareaProyecto = tareaProyecto;
    }
}