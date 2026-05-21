package com.example.demo.dto;
/**
 *  DTO (Data Transfer Object) para representar los tiempos de un proyecto en las respuestas de la API.
*/
/**
 * Clase que contiene un resumen los tiempos de un proyecto para su envio a front
 */
public class ResumenTiemposDTO {
    private Double tiempoRealTotal;
    private Double tiempoEstimadoMedia;

    public ResumenTiemposDTO(Double tiempoRealTotal, Double tiempoEstimadoMedia) {
        this.tiempoRealTotal = tiempoRealTotal;
        this.tiempoEstimadoMedia = tiempoEstimadoMedia;
    }

    public Double getTiempoRealTotal() { return tiempoRealTotal; }
    public void setTiempoRealTotal(Double tiempoRealTotal) { this.tiempoRealTotal = tiempoRealTotal; }

    public Double getTiempoEstimadoMedia() { return tiempoEstimadoMedia; }
    public void setTiempoEstimadoMedia(Double tiempoEstimadoMedia) { this.tiempoEstimadoMedia = tiempoEstimadoMedia; }
}