package com.example.demo.dto.excel;

public class FilaValidacionGitlabDTO {
    private String idGitlab;
    private String nombreGitlab;
    private String nombreProyecto;
    private String estado;
    private boolean vinculada;

    public FilaValidacionGitlabDTO() {}

    public String getIdGitlab() { return idGitlab; }
    public void setIdGitlab(String idGitlab) { this.idGitlab = idGitlab; }

    public String getNombreGitlab() { return nombreGitlab; }
    public void setNombreGitlab(String nombreGitlab) { this.nombreGitlab = nombreGitlab; }

    public String getNombreProyecto() { return nombreProyecto; }
    public void setNombreProyecto(String nombreProyecto) { this.nombreProyecto = nombreProyecto; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public boolean isVinculada() { return vinculada; }
    public void setVinculada(boolean vinculada) { this.vinculada = vinculada; }
}