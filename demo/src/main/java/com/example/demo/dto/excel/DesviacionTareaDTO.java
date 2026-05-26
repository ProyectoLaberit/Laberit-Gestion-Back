package com.example.demo.dto.excel;

public class DesviacionTareaDTO {
    private String idGitlab;
    private String fase;
    private String tarea;
    private String departamento;
    private Double estMin;
    private Double estMax;
    private Double horasReales;
    private Double desviacion;
    private String estadoSalud; 
    private String estadoGitlab;

    public DesviacionTareaDTO() {
    }

    // Getters y Setters
    public String getIdGitlab() { return idGitlab; }
    public void setIdGitlab(String idGitlab) { this.idGitlab = idGitlab; }

    public String getFase() { return fase; }
    public void setFase(String fase) { this.fase = fase; }

    public String getTarea() { return tarea; }
    public void setTarea(String tarea) { this.tarea = tarea; }

    public String getDepartamento() { return departamento; }
    public void setDepartamento(String departamento) { this.departamento = departamento; }

    public Double getEstMin() { return estMin; }
    public void setEstMin(Double estMin) { this.estMin = estMin; }

    public Double getEstMax() { return estMax; }
    public void setEstMax(Double estMax) { this.estMax = estMax; }

    public Double getHorasReales() { return horasReales; }
    public void setHorasReales(Double horasReales) { this.horasReales = horasReales; }

    public Double getDesviacion() { return desviacion; }
    public void setDesviacion(Double desviacion) { this.desviacion = desviacion; }

    public String getEstadoSalud() { return estadoSalud; }
    public void setEstadoSalud(String estadoSalud) { this.estadoSalud = estadoSalud; }

    public String getEstadoGitlab() { return estadoGitlab; }
    public void setEstadoGitlab(String estadoGitlab) { this.estadoGitlab = estadoGitlab; }
}
