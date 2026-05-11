package com.example.demo.dto;

public class ResumenTiemposDTO {
    private Double tiempoRealTotal;
    private Double tiempoEstimadoMedia;

    public ResumenTiemposDTO(Double tiempoRealTotal, Double tiempoEstimadoMedia) {
        this.tiempoRealTotal = tiempoRealTotal;
        this.tiempoEstimadoMedia = tiempoEstimadoMedia;
    }

    // Getters y Setters
    public Double getTiempoRealTotal() { return tiempoRealTotal; }
    public void setTiempoRealTotal(Double tiempoRealTotal) { this.tiempoRealTotal = tiempoRealTotal; }

    public Double getTiempoEstimadoMedia() { return tiempoEstimadoMedia; }
    public void setTiempoEstimadoMedia(Double tiempoEstimadoMedia) { this.tiempoEstimadoMedia = tiempoEstimadoMedia; }
}