package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
/**
 * Entidad que representa los excel almacenados en la base de datos
 */

@Entity
@Table(name = "excel")
public class Excel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_excel")
    private Integer idExcel;

    @Column(name = "id_proyecto", nullable = false)
    private Long idProyecto;

    @Column(name = "id_usuario", nullable = false)
    private Integer idUsuario;

    @Column(name = "fecha_subida", nullable = false)
    private LocalDate fechaSubida;

    @Column(name = "ruta_archivo", length = 2000, nullable = false)
    private String rutaArchivo;

    // Aquí está nuestra variable estrella
    @Column(name = "vigente", nullable = false)
    private Boolean vigente = false;

    // ==========================================================
    // CONSTRUCTOR
    // ==========================================================
    public Excel() {
    }

    // ==========================================================
    // GETTERS Y SETTERS
    // ==========================================================

    public Integer getIdExcel() {
        return idExcel;
    }

    public void setIdExcel(Integer idExcel) {
        this.idExcel = idExcel;
    }

    public Long getIdProyecto() {
        return idProyecto;
    }

    public void setIdProyecto(Long idProyecto) {
        this.idProyecto = idProyecto;
    }

    public Integer getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Integer idUsuario) {
        this.idUsuario = idUsuario;
    }

    public LocalDate getFechaSubida() {
        return fechaSubida;
    }

    public void setFechaSubida(LocalDate fechaSubida) {
        this.fechaSubida = fechaSubida;
    }

    public String getRutaArchivo() {
        return rutaArchivo;
    }

    public void setRutaArchivo(String rutaArchivo) {
        this.rutaArchivo = rutaArchivo;
    }

    public Boolean getVigente() {
        return vigente;
    }

    public void setVigente(Boolean vigente) {
        this.vigente = vigente;
    }
}