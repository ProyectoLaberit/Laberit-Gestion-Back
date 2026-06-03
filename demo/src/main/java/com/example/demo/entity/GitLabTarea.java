package com.example.demo.entity;

import jakarta.persistence.*;

/**
 * Entidad que representa las tareas de GitLab sincronizadas y persistidas en la
 * base de datos.
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

    @Column(name = "id_departamento")
    private Integer idDepartamento;

    @Column(name = "estado", nullable = false, length = 50)
    private String estado;

    @Column(name = "valida", nullable = false)
    private Boolean valida = false;

    @Column(name = "id_tarea_proyecto")
    private Long tareaProyecto;

    @Column(name = "id_proyecto")
    private Long idProyecto;

    // Constructor vacío requerido por JPA
    public GitLabTarea() {
    }

    // Constructor completo para pruebas y factorías
    public GitLabTarea(Long id, String issueId, Long numeroGitlab, String titulo, Integer idDepartamento,
            String estado, Boolean valida, Long idProyecto, Long tareaProyecto) {
        this.id = id;
        this.issueId = issueId;
        this.numeroGitlab = numeroGitlab;
        this.titulo = titulo;
        this.idDepartamento = idDepartamento;
        this.estado = estado;
        this.valida = valida;
        this.idProyecto = idProyecto;
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

    public Integer getIdDepartamento() {
        return idDepartamento;
    }

    public void setIdDepartamento(Integer idDepartamento) {
        this.idDepartamento = idDepartamento;
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