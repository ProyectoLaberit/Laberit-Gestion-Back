package com.example.demo.dto;

public class DetalleEstimacionDTO {
    private Long id;
    private Integer idDepartamento;
    private Long idProyecto;
    private Integer idFase;
    private String tarea;
    private Double tiempoMax;
    private Double tiempoMin;

    public DetalleEstimacionDTO() {
    }

    public DetalleEstimacionDTO(Long id, Integer idDepartamento, Long idProyecto, Integer idFase, String tarea, Double tiempoMax, Double tiempoMin) {
        this.id = id;
        this.idDepartamento = idDepartamento;
        this.idProyecto = idProyecto;
        this.idFase = idFase;
        this.tarea = tarea;
        this.tiempoMax = tiempoMax;
        this.tiempoMin = tiempoMin;
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getIdDepartamento() { return idDepartamento; }
    public void setIdDepartamento(Integer idDepartamento) { this.idDepartamento = idDepartamento; }

    public Long getIdProyecto() { return idProyecto; }
    public void setIdProyecto(Long idProyecto) { this.idProyecto = idProyecto; }

    public Integer getIdFase() { return idFase; }
    public void setIdFase(Integer idFase) { this.idFase = idFase; }

    public String getTarea() { return tarea; }
    public void setTarea(String tarea) { this.tarea = tarea; }

    public Double getTiempoMax() { return tiempoMax; }
    public void setTiempoMax(Double tiempoMax) { this.tiempoMax = tiempoMax; }

    public Double getTiempoMin() { return tiempoMin; }
    public void setTiempoMin(Double tiempoMin) { this.tiempoMin = tiempoMin; }
}
