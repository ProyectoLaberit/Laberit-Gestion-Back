package com.example.demo.dto.excel;

public class FilaGitLabTareaDTO {
    private String idGitlab;
    private String fase;
    private String tarea;
    private String departamento;
    private int estimacionMinima;
    private int estimacionMaxima;
    private int horasReales;
    private int desviacionHoras;
    private String estadoGitlab;
    private double desviacionPorcentaje;

    // Getters y Setters
    public String getIdGitlab() { return idGitlab; }
    public void setIdGitlab(String idGitlab) { this.idGitlab = idGitlab; }

    public String getFase() { return fase; }
    public void setFase(String fase) { this.fase = fase; }

    public String getTarea() { return tarea; }
    public void setTarea(String tarea) { this.tarea = tarea; }

    public String getDepartamento() { return departamento; }
    public void setDepartamento(String departamento) { this.departamento = departamento; }

    public int getEstimacionMinima() { return estimacionMinima; }
    public void setEstimacionMinima(int estimacionMinima) { this.estimacionMinima = estimacionMinima; }

    public int getEstimacionMaxima() { return estimacionMaxima; }
    public void setEstimacionMaxima(int estimacionMaxima) { this.estimacionMaxima = estimacionMaxima; }

    public int getHorasReales() { return horasReales; }
    public void setHorasReales(int horasReales) { this.horasReales = horasReales; }

    public int getDesviacionHoras() { return desviacionHoras; }
    public void setDesviacionHoras(int desviacionHoras) { this.desviacionHoras = desviacionHoras; }

    public String getEstadoGitlab() { return estadoGitlab; }
    public void setEstadoGitlab(String estadoGitlab) { this.estadoGitlab = estadoGitlab; }

    public double getDesviacionPorcentaje() { return desviacionPorcentaje; }
    public void setDesviacionPorcentaje(double desviacionPorcentaje) { this.desviacionPorcentaje = desviacionPorcentaje; }
}