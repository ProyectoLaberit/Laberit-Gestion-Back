package com.example.demo.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "tarea_gitlab") // Mapea directamente con tu tabla real en Neon
public class GitLabTarea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tarea_gitlab") // Tu clave primaria autoincremental (BIGSERIAL)
    private Long idTareaGitlab;

    @Column(name = "issue_id", nullable = false, length = 100) // El ID global de GitLab que viene en tu DTO (String)
    private String issueId;

    @Column(name = "iid_gitlab", nullable = false) // El número visible con almohadilla (#14) que guardamos como número
                                                   // (Long)
    private Long iidGitlab;

    @Column(name = "titulo", nullable = false) // Título de la issue
    private String titulo;

    @Column(name = "estado", nullable = false, length = 50) // Estado de la issue ('opened', 'closed'...)
    private String estado;

    @Column(name = "url", length = 500) // La columna 'url' que tienes en tu captura de pantalla
    private String url;

    // Relación ManyToOne con la tabla central real de tu base de datos
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tarea_proyecto", nullable = false) // Tu columna FK real de la captura
    private TareaProyecto tareaProyecto;

    // --- CONSTRUCTORES ---

    public GitLabTarea() {
    }

    public GitLabTarea(Long idTareaGitlab, String issueId, Long iidGitlab, String titulo, String estado, String url,
            TareaProyecto tareaProyecto) {
        this.idTareaGitlab = idTareaGitlab;
        this.issueId = issueId;
        this.iidGitlab = iidGitlab;
        this.titulo = titulo;
        this.estado = estado;
        this.url = url;
        this.tareaProyecto = tareaProyecto;
    }

    // --- GETTERS Y SETTERS ---

    public Long getIdTareaGitlab() {
        return idTareaGitlab;
    }

    public void setIdTareaGitlab(Long idTareaGitlab) {
        this.idTareaGitlab = idTareaGitlab;
    }

    public String getIssueId() {
        return issueId;
    }

    public void setIssueId(String issueId) {
        this.issueId = issueId;
    }

    public Long getIidGitlab() {
        return iidGitlab;
    }

    public void setIidGitlab(Long iidGitlab) {
        this.iidGitlab = iidGitlab;
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