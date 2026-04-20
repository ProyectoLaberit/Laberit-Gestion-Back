package com.example.demo.dto;

public class TareaSubfaseDTO {

    private String nombreTarea;
    private double tiempoTotalMin;
    private double tiempoTotalMax;

    public TareaSubfaseDTO() {
    }

    public TareaSubfaseDTO(String nombreTarea, double tiempoTotalMin, double tiempoTotalMax) {
        this.nombreTarea = nombreTarea;
        this.tiempoTotalMin = tiempoTotalMin;
        this.tiempoTotalMax = tiempoTotalMax;
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
    
}
