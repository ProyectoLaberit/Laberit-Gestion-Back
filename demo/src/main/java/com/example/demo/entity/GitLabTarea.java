package com.example.demo.entity;

import jakarta.persistence.*;
/**
 * Entidad que representa las tareas de gitlab de la base de datos
 * tiene una relacion recursiva con "tarea_proyecto"
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

    @Column(name = "url", length = 500)
    private String url;

    // Relación ManyToOne con la tabla central real de tu base de datos
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tarea_proyecto", nullable = false)
    private TareaProyecto tareaProyecto;

    public GitLabTarea() {
    }

    public GitLabTarea(Long id, String issueId, Long numeroGitLab, String titulo, String estado, String url,
            TareaProyecto tareaProyecto) {
        this.id = id;
        this.issueId = issueId;
        this.numeroGitLab = numeroGitLab;
        this.titulo = titulo;
        this.estado = estado;
        this.url = url;
        this.tareaProyecto = tareaProyecto;
    }

    public Long getId() {
        return id;
    }

    public void setIdTareaGitlab(Long id) {
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

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public TareaProyecto getTareaProyecto() {
        return tareaProyecto;
    }

    public void setTareaProyecto(TareaProyecto tareaProyecto) {
        this.tareaProyecto = tareaProyecto;
    }
}