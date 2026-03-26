package com.example.demo.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "detalle_estimacion")
public class DetalleEstimacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detalle_estimacion")
    private Long id;

    @Column(name = "id_proyecto")
    private Long idProyecto;

    private String tarea;

    @Column(name = "tiempo_max")
    private Double tiempoMax;

    @Column(name = "tiempo_min")
    private Double tiempoMin;

    // Constructor vacío obligatorio
    public DetalleEstimacion() {}

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getIdProyecto() { return idProyecto; }
    public void setIdProyecto(Long idProyecto) { this.idProyecto = idProyecto; }

    public String getTarea() { return tarea; }
    public void setTarea(String tarea) { this.tarea = tarea; }

    public Double getTiempoMax() { return tiempoMax; }
    public void setTiempoMax(Double tiempoMax) { this.tiempoMax = tiempoMax; }

    public Double getTiempoMin() { return tiempoMin; }
    public void setTiempoMin(Double tiempoMin) { this.tiempoMin = tiempoMin; }
}
