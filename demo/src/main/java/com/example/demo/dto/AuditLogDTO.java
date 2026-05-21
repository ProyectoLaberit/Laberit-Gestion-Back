package com.example.demo.dto;

import java.time.LocalDateTime;
/**
 * Clase que contiene las acciones de un usuario sobre otro y la informacion de ambos
 */

public class AuditLogDTO {

    private Long id;
    private String accion;
    private String descripcion;
    private LocalDateTime fechaHora;
    private Integer idUsuarioActor;
    private String usuarioNombre;
    private String usuarioEmail;
    private Integer idUsuarioObjetivo;
    private String usuarioObjetivoNombre;
    private String usuarioObjetivoEmail;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAccion() {
        return accion;
    }

    public void setAccion(String accion) {
        this.accion = accion;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }

    public Integer getIdUsuarioActor() {
        return idUsuarioActor;
    }

    public void setIdUsuarioActor(Integer idUsuarioActor) {
        this.idUsuarioActor = idUsuarioActor;
    }

    public String getUsuarioNombre() {
        return usuarioNombre;
    }

    public void setUsuarioNombre(String usuarioNombre) {
        this.usuarioNombre = usuarioNombre;
    }

    public String getUsuarioEmail() {
        return usuarioEmail;
    }

    public void setUsuarioEmail(String usuarioEmail) {
        this.usuarioEmail = usuarioEmail;
    }

    public Integer getIdUsuarioObjetivo() {
        return idUsuarioObjetivo;
    }

    public void setIdUsuarioObjetivo(Integer idUsuarioObjetivo) {
        this.idUsuarioObjetivo = idUsuarioObjetivo;
    }

    public String getUsuarioObjetivoNombre() {
        return usuarioObjetivoNombre;
    }

    public void setUsuarioObjetivoNombre(String usuarioObjetivoNombre) {
        this.usuarioObjetivoNombre = usuarioObjetivoNombre;
    }

    public String getUsuarioObjetivoEmail() {
        return usuarioObjetivoEmail;
    }

    public void setUsuarioObjetivoEmail(String usuarioObjetivoEmail) {
        this.usuarioObjetivoEmail = usuarioObjetivoEmail;
    }
}
