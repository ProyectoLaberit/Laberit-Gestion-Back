package com.example.demo.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "tarea_proyecto")
public class TareaProyecto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tarea_proyecto")
    private Long id;

    @Column(name = "id_proyecto", nullable = false)
    private Long idProyecto;

    @Column(name = "id_fase", nullable = false)
    private Integer idFase;

    @Column(name = "id_departamento", nullable = false)
    private Integer idDepartamento;

    @Column(name = "tarea", nullable = false)
    private String tarea;

    @Column(name = "completada", nullable = false)
    private Boolean completada = false;

    public TareaProyecto() {}

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getIdProyecto() { return idProyecto; }
    public void setIdProyecto(Long idProyecto) { this.idProyecto = idProyecto; }

    public Integer getIdFase() { return idFase; }
    public void setIdFase(Integer idFase) { this.idFase = idFase; }

    public Integer getIdDepartamento() { return idDepartamento; }
    public void setIdDepartamento(Integer idDepartamento) { this.idDepartamento = idDepartamento; }

    public String getTarea() { return tarea; }
    public void setTarea(String tarea) { this.tarea = tarea; }

    public Boolean getCompletada() { return completada; }
    public void setCompletada(Boolean completada) { this.completada = completada; }
}