package com.example.demo.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Clase enviada y/o recibida del front para mostrar o recibir datos 
 */
public class HistorialExcelDTO {
    private Integer idExcel;
    private LocalDate fechaSubida;
    private String usuarioNombre;
    private Boolean vigente;
    // Constructor
   public HistorialExcelDTO(Integer idExcel, LocalDate fechaSubida, String usuarioNombre, Boolean vigente) {
        this.idExcel = idExcel;
        this.fechaSubida = fechaSubida;
        this.usuarioNombre = usuarioNombre;
        this.vigente = vigente;
    }

    // Getters y Setters
    public Integer getIdExcel() { return idExcel; }
    public void setIdExcel(Integer idExcel) { this.idExcel = idExcel; }
    public LocalDate getFechaSubida() { return fechaSubida; }
    public void setFechaSubida(LocalDate fechaSubida) { this.fechaSubida = fechaSubida; }
    public String getUsuarioNombre() { return usuarioNombre; }
    public void setUsuarioNombre(String usuarioNombre) { this.usuarioNombre = usuarioNombre; }
    public Boolean isVigente() { return vigente; }
    public void setVigente(Boolean vigente) { this.vigente = vigente; }
}
