package com.example.demo.dto;

public class ClockifyTareaDTO {
    private String idGitlab;
    private String faseExcel;
    private String titulo;
    private double horasTrabajadas;

    public ClockifyTareaDTO() {
    }

    public ClockifyTareaDTO(String idGitlab, String faseExcel, String titulo, double horasTrabajadas) {
        this.idGitlab = idGitlab;
        this.faseExcel = faseExcel;
        this.titulo = titulo;
        this.horasTrabajadas = horasTrabajadas;
    }

    public String getIdGitlab() {
        return idGitlab;
    }

    public void setIdGitlab(String idGitlab) {
        this.idGitlab = idGitlab;
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

    
}
