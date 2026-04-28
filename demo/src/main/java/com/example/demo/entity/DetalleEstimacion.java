package com.example.demo.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "detalle_estimacion")
public class DetalleEstimacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_excel")
    private Integer idExcel;

    @Column(name = "id_fase")
    private Integer idFase; // Este ID apunta a la Subfase (ej: el 4 de Análisis)

    @Column(name = "id_departamento")
    private Integer idDepartamento;

    @Column(name = "tarea")
    private String tarea;

    @Column(name = "tiempo_min")
    private Double tiempoMin;

    @Column(name = "tiempo_max")
    private Double tiempoMax;

    @Column(name = "tiempo_real", nullable = true)
    private Double tiempoReal;

    @Column(name = "gitlab_issue_id", length = 45, nullable = true)
    private String gitlabIssueId;

    // Constructores, Getters y Setters
    public DetalleEstimacion() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getIdExcel() { return idExcel; }
    public void setIdExcel(Integer idExcel) { this.idExcel = idExcel; }
    public Integer getIdFase() { return idFase; }
    public void setIdFase(Integer idFase) { this.idFase = idFase; }
    public Integer getIdDepartamento() { return idDepartamento; }
    public void setIdDepartamento(Integer idDepartamento) { this.idDepartamento = idDepartamento; }
    public String getTarea() { return tarea; }
    public void setTarea(String tarea) { this.tarea = tarea; }
    public Double getTiempoMin() { return tiempoMin; }
    public void setTiempoMin(Double tiempoMin) { this.tiempoMin = tiempoMin; }
    public Double getTiempoMax() { return tiempoMax; }
    public void setTiempoMax(Double tiempoMax) { this.tiempoMax = tiempoMax; }
    public Double getTiempoReal() { return tiempoReal; }
    public void setTiempoReal(Double tiempoReal) { this.tiempoReal = tiempoReal; }
    public String getGitlabIssueId() { return gitlabIssueId; }
    public void setGitlabIssueId(String gitlabIssueId) { this.gitlabIssueId = gitlabIssueId; }
}