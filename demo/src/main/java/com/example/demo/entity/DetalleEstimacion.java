package com.example.demo.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "detalle_estimacion")
public class DetalleEstimacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_excel", nullable = false)
    private Integer idExcel;

    @Column(name = "id_tarea_proyecto", nullable = false)
    private Long idTareaProyecto;

    @Column(name = "tiempo_min")
    private Double tiempoMin;

    @Column(name = "tiempo_max")
    private Double tiempoMax;

    public DetalleEstimacion() {}

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getIdExcel() { return idExcel; }
    public void setIdExcel(Integer idExcel) { this.idExcel = idExcel; }

    public Long getIdTareaProyecto() { return idTareaProyecto; }
    public void setIdTareaProyecto(Long idTareaProyecto) { this.idTareaProyecto = idTareaProyecto; }

    public Double getTiempoMin() { return tiempoMin; }
    public void setTiempoMin(Double tiempoMin) { this.tiempoMin = tiempoMin; }

    public Double getTiempoMax() { return tiempoMax; }
    public void setTiempoMax(Double tiempoMax) { this.tiempoMax = tiempoMax; }

    @Transient
public String getTarea() {
    return "Presupuesto ID: " + this.id;
}
}