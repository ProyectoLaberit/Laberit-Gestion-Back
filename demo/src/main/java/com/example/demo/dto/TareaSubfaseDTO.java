package com.example.demo.dto;

/**
 * Clase enviada y/o recibida del front para mostrar o recibir datos 
 */
public class TareaSubfaseDTO {

    private Long idTarea;
    private String nombreTarea;
    private double tiempoTotalMin;
    private double tiempoTotalMax;
    private Double tiempoTotalMinElegido;
    private Double tiempoTotalMaxElegido;

    public TareaSubfaseDTO() {
    }

    public TareaSubfaseDTO(String nombreTarea, double tiempoTotalMin, double tiempoTotalMax) {
        this.nombreTarea = nombreTarea;
        this.tiempoTotalMin = tiempoTotalMin;
        this.tiempoTotalMax = tiempoTotalMax;
    }

    public Long getIdTarea() {
        return idTarea;
    }

    public void setIdTarea(Long idTarea) {
        this.idTarea = idTarea;
    }

    public String getNombreTarea() {
        return nombreTarea;
    }

    public void setNombreTarea(String nombreTarea) {
        this.nombreTarea = nombreTarea;
    }

    public double getTiempoTotalMin() {
        return tiempoTotalMin;
    }

    public void setTiempoTotalMin(double tiempoTotalMin) {
        this.tiempoTotalMin = tiempoTotalMin;
    }

    public double getTiempoTotalMax() {
        return tiempoTotalMax;
    }

    public void setTiempoTotalMax(double tiempoTotalMax) {
        this.tiempoTotalMax = tiempoTotalMax;
    }

    public Double getTiempoTotalMinElegido() { 
        return tiempoTotalMinElegido; 
    }

    public void setTiempoTotalMinElegido(Double tiempoTotalMinElegido) { 
        this.tiempoTotalMinElegido = tiempoTotalMinElegido; 
    }

    public Double getTiempoTotalMaxElegido() { 
        return tiempoTotalMaxElegido; 
    }

    public void setTiempoTotalMaxElegido(Double tiempoTotalMaxElegido) { 
        this.tiempoTotalMaxElegido = tiempoTotalMaxElegido; 
    }

}
