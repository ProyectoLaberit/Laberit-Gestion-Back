package com.example.demo.dto.excel;

public class FilaValidacionGitlabDTO {
    private String idGitlab;
    private String nombreGitlab;
    private String nombreTareaProyecto; 
    private Boolean tareaInternaCompletada; 
    private boolean vinculada;

    // Getters y Setters
    public String getIdGitlab() { return idGitlab; }
    public void setIdGitlab(String idGitlab) { this.idGitlab = idGitlab; }

    public String getNombreGitlab() { return nombreGitlab; }
    public void setNombreGitlab(String nombreGitlab) { this.nombreGitlab = nombreGitlab; }

    public String getNombreTareaProyecto() { return nombreTareaProyecto; }
    public void setNombreTareaProyecto(String nombreTareaProyecto) { this.nombreTareaProyecto = nombreTareaProyecto; }

    public Boolean getTareaInternaCompletada() { return tareaInternaCompletada; }
    public void setTareaInternaCompletada(Boolean tareaInternaCompletada) { this.tareaInternaCompletada = tareaInternaCompletada; }

    public boolean isVinculada() { return vinculada; }
    public void setVinculada(boolean vinculada) { this.vinculada = vinculada; }
}