package com.example.demo.dto;

import java.time.LocalDateTime;

public class HistorialExcelDTO {
    private Long idExcel;
    private LocalDateTime fechaSubida;
    private String usuarioNombre;
    private boolean vigente;

    // Constructor
    public HistorialExcelDTO(Long idExcel, LocalDateTime fechaSubida, String usuarioNombre, boolean vigente) {
        this.idExcel = idExcel;
        this.fechaSubida = fechaSubida;
        this.usuarioNombre = usuarioNombre;
        this.vigente = vigente;
    }

    // Getters y Setters
    public Long getIdExcel() { return idExcel; }
    public void setIdExcel(Long idExcel) { this.idExcel = idExcel; }
    public LocalDateTime getFechaSubida() { return fechaSubida; }
    public void setFechaSubida(LocalDateTime fechaSubida) { this.fechaSubida = fechaSubida; }
    public String getUsuarioNombre() { return usuarioNombre; }
    public void setUsuarioNombre(String usuarioNombre) { this.usuarioNombre = usuarioNombre; }
    public boolean isVigente() { return vigente; }
    public void setVigente(boolean vigente) { this.vigente = vigente; }
}
