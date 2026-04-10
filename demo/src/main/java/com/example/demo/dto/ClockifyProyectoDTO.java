package com.example.demo.dto;

public class ClockifyProyectoDTO {
    private String nombreProyecto;
    private String faseExcel;
    private String titulo;
    private double horasTrabajadas;

    public ClockifyProyectoDTO() {
    }

    public ClockifyProyectoDTO(String nombreProyecto, String faseExcel, String titulo,
            double horasTrabajadas) {
        this.nombreProyecto = nombreProyecto;
        this.faseExcel = faseExcel;
        this.titulo = titulo;
        this.horasTrabajadas = horasTrabajadas;
    }

    public String getFaseExcel() {
        return faseExcel;
    }

    public void setFaseExcel(String faseExcel) {
        this.faseExcel = faseExcel;
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

    public String getNombreProyecto() {
        return nombreProyecto;
    }

    public void setNombreProyecto(String nombreProyecto) {
        this.nombreProyecto = nombreProyecto;
    }

}
