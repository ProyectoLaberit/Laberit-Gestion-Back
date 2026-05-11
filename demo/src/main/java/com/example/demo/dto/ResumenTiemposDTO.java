package com.example.demo.dto;

import java.time.LocalTime;

public class ResumenTiemposDTO {
    private LocalTime tiempoRealTotal;
    private LocalTime tiempoEstimadoMedia;

    public ResumenTiemposDTO(LocalTime tiempoRealTotal, LocalTime tiempoEstimadoMedia) {
        this.tiempoRealTotal = tiempoRealTotal;
        this.tiempoEstimadoMedia = tiempoEstimadoMedia;
    }

    // Getters y Setters
    public LocalTime getTiempoRealTotal() { return tiempoRealTotal; }
    public void setTiempoRealTotal(LocalTime tiempoRealTotal) { this.tiempoRealTotal = tiempoRealTotal; }

    public LocalTime getTiempoEstimadoMedia() { return tiempoEstimadoMedia; }
    public void setTiempoEstimadoMedia(LocalTime tiempoEstimadoMedia) { this.tiempoEstimadoMedia = tiempoEstimadoMedia; }
}