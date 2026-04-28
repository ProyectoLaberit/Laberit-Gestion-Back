package com.example.demo.dto;

public class ClockifyTareaDTO {
    private String titulo;
    private double horasTrabajadas;
    private int idGit;
    private String departamento;

    //nombre tiempos
    public ClockifyTareaDTO() {
    }

    public ClockifyTareaDTO( String titulo, double horasTrabajadas, int idGit, String departamento) {
        this.titulo = titulo;
        this.horasTrabajadas = horasTrabajadas;
        this.idGit = idGit;
        this.departamento = departamento;
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

    public String getDepartamento() {
        return departamento;
    }

    public void setDepartamento(String departamento) {
        this.departamento = departamento;
    }

    

    
}
