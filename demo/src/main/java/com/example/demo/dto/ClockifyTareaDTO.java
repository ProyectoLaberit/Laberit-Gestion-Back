package com.example.demo.dto;

import java.util.Date;
/**
 *  DTO (Data Transfer Object) para representar una tarea de clockify en las respuestas de la API.
*/
/**
 * Clase enviada y/o recibida del front para mostrar o recibir datos de las tareas de clockify
 */
public class ClockifyTareaDTO {
    private String titulo;
    private double horasTrabajadas;
    private String departamento;
    private String descripcion;
    private Date fecha;
    private double inicio;
    private double fin;

    public ClockifyTareaDTO() {
    }

    public ClockifyTareaDTO( String titulo, double horasTrabajadas, String departamento, String descripcion, Date fecha, double inicio, double fin) {
        this.titulo = titulo;
        this.horasTrabajadas = horasTrabajadas;
        this.departamento = departamento;
        this.descripcion = descripcion;
        this.fecha = fecha;
        this.inicio = inicio;
        this.fin = fin; 
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public double getHorasTrabajadas() {
        return horasTrabajadas;
    }

    public void setHorasTrabajadas(double horasTrabajadas) {
        this.horasTrabajadas = horasTrabajadas;
    }

    public String getDepartamento() {
        return departamento;
    }

    public void setDepartamento(String departamento) {
        this.departamento = departamento;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Date getFecha() {
        return fecha;
    }

    public void setFecha(Date fecha) {
        this.fecha = fecha;
    }

    public double getInicio() {
        return inicio;
    }

    public void setInicio(double inicio) {
        this.inicio = inicio;
    }

    public double getFin() {
        return fin;
    }

    public void setFin(double fin) {
        this.fin = fin;
    }

    

    
}
