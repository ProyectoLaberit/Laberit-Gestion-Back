package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Tipo de acción: IMPORTACION_EXCEL, CAMBIO_ESTIMACION, SINCRONIZACION, CREACION_TAREA, etc.
    @Column(name = "accion", nullable = false, length = 100)
    private String accion;

    // Descripción legible del evento
    @Column(name = "descripcion", length = 500)
    private String descripcion;

    // Email del usuario que realizó la acción
    @Column(name = "usuario_email", nullable = false, length = 255)
    private String usuarioEmail;

    // Nombre del usuario
    @Column(name = "usuario_nombre", length = 255)
    private String usuarioNombre;

    // Fecha y hora exacta
    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    // ID del proyecto afectado (opcional)
    @Column(name = "id_proyecto")
    private Long idProyecto;

    // Metadatos adicionales en texto libre (JSON string, opcional)
    @Column(name = "extra", length = 1000)
    private String extra;

    public AuditLog() {}

    // Constructor de conveniencia
    public AuditLog(String accion, String descripcion, String usuarioEmail,
                    String usuarioNombre, Long idProyecto) {
        this.accion        = accion;
        this.descripcion   = descripcion;
        this.usuarioEmail  = usuarioEmail;
        this.usuarioNombre = usuarioNombre;
        this.idProyecto    = idProyecto;
        this.fechaHora     = LocalDateTime.now();
    }

    // Getters y Setters
    public Long getId()                     { return id; }
    public String getAccion()               { return accion; }
    public void setAccion(String accion)    { this.accion = accion; }
    public String getDescripcion()          { return descripcion; }
    public void setDescripcion(String d)    { this.descripcion = d; }
    public String getUsuarioEmail()         { return usuarioEmail; }
    public void setUsuarioEmail(String e)   { this.usuarioEmail = e; }
    public String getUsuarioNombre()        { return usuarioNombre; }
    public void setUsuarioNombre(String n)  { this.usuarioNombre = n; }
    public LocalDateTime getFechaHora()     { return fechaHora; }
    public void setFechaHora(LocalDateTime f) { this.fechaHora = f; }
    public Long getIdProyecto()             { return idProyecto; }
    public void setIdProyecto(Long id)      { this.idProyecto = id; }
    public String getExtra()                { return extra; }
    public void setExtra(String extra)      { this.extra = extra; }
}
