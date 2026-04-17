package com.example.demo.dto;

public class ClockifyTareaDTO {
    private String titulo;
    private double horasTrabajadas;
    private int idGit;

    //nombre tiempos
    public ClockifyTareaDTO() {
    }

    public ClockifyTareaDTO( String titulo, double horasTrabajadas, int idGit) {
        this.titulo = titulo;
        this.horasTrabajadas = horasTrabajadas;
        this.idGit = idGit;
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

    public int getIdGit() {
        return idGit;
    }

    public void setIdGit(int idGit) {
        this.idGit = idGit;
    }

    
}
