package com.example.demo.dto;
/**
 *  DTO (Data Transfer Object) para representar los tiempos de un proyecto en las respuestas de la API.
*/
/**
 * Clase que contiene un resumen los tiempos de un proyecto para su envio a front
 */
public class ResumenTiemposDTO {
    private Double tiempoRealTotal;
    private Double tiempoEstimadoMin;
    private Double tiempoEstimadoMax;

    public ResumenTiemposDTO(Double tiempoRealTotal, Double tiempoEstimadoMin, Double tiempoEstimadoMax) {
        this.tiempoRealTotal = tiempoRealTotal;
        this.tiempoEstimadoMin = tiempoEstimadoMin;
        this.tiempoEstimadoMax = tiempoEstimadoMax;
    }

    public Double getTiempoRealTotal() { return tiempoRealTotal; }
    public void setTiempoRealTotal(Double tiempoRealTotal) { this.tiempoRealTotal = tiempoRealTotal; }

    public Double getTiempoEstimadoMin() { return tiempoEstimadoMin; }
    public void setTiempoEstimadoMin(Double tiempoEstimadoMin) { this.tiempoEstimadoMin = tiempoEstimadoMin; }

    public Double getTiempoEstimadoMax() { return tiempoEstimadoMax; }
    public void setTiempoEstimadoMax(Double tiempoEstimadoMax) { this.tiempoEstimadoMax = tiempoEstimadoMax; }
}