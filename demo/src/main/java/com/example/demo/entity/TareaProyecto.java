package com.example.demo.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "tarea_proyecto", schema = "public")
public class TareaProyecto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tarea_proyecto")
    private Long idTareaProyecto;

    @Column(name = "id_proyecto", nullable = false)
    private Long idProyecto; // BIGINT en tu base de datos

    @Column(name = "id_fase", nullable = false)
    private Integer idFase; // INTEGER en tu base de datos

    @Column(name = "id_departamento", nullable = false)
    private Integer idDepartamento; // INTEGER en tu base de datos

    @Column(name = "tarea", nullable = false, length = 255) // VARCHAR(255) en tu base de datos
    private String tarea;

    @Column(name = "completada", nullable = false)
    private Boolean completada = false; // BOOLEAN DEFAULT false

    // --- CONSTRUCTORES ---
    public TareaProyecto() {
    }

    public TareaProyecto(Long idTareaProyecto, Long idProyecto, Integer idFase, Integer idDepartamento, String tarea,
            Boolean completada) {
        this.idTareaProyecto = idTareaProyecto;
        this.idProyecto = idProyecto;
        this.idFase = idFase;
        this.idDepartamento = idDepartamento;
        this.tarea = tarea;
        this.completada = completada;
    }

    // --- GETTERS Y SETTERS ---
    public Long getIdTareaProyecto() {
        return idTareaProyecto;
    }

    public void setIdTareaProyecto(Long idTareaProyecto) {
        this.idTareaProyecto = idTareaProyecto;
    }

    public Long getIdProyecto() {
        return idProyecto;
    }

    public void setIdProyecto(Long idProyecto) {
        this.idProyecto = idProyecto;
    }

    public Integer getIdFase() {
        return idFase;
    }

    public void setIdFase(Integer idFase) {
        this.idFase = idFase;
    }

    public Integer getIdDepartamento() {
        return idDepartamento;
    }

    public void setIdDepartamento(Integer idDepartamento) {
        this.idDepartamento = idDepartamento;
    }

    public String getTarea() {
        return tarea;
    }

    public void setTarea(String tarea) {
        this.tarea = tarea;
    }

    public Boolean getCompletada() {
        return completada;
    }

    public void setCompletada(Boolean completada) {
        this.completada = completada;
    }
}