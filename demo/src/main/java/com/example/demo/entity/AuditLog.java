package com.example.demo.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

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

    // ID del usuario que realizó la acción
    @Column(name = "id_usuario")
    private Integer idUsuario;

    // Relación opcional para resolver datos vivos del usuario sin romper históricos si se elimina
    @JsonIgnore
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_usuario", referencedColumnName = "id_usuario", insertable = false, updatable = false,
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    @NotFound(action = NotFoundAction.IGNORE)
    private Usuario usuario;

    // ID del usuario afectado por la acción (si aplica)
    @Column(name = "id_usuario_objetivo")
    private Integer idUsuarioObjetivo;

    // Relación opcional al usuario afectado
    @JsonIgnore
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_usuario_objetivo", referencedColumnName = "id_usuario", insertable = false, updatable = false,
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    @NotFound(action = NotFoundAction.IGNORE)
    private Usuario usuarioObjetivo;

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
    public AuditLog(String accion, String descripcion, Integer idUsuario, String usuarioEmail,
                    String usuarioNombre, Long idProyecto, Integer idUsuarioObjetivo) {
        this.accion        = accion;
        this.descripcion   = descripcion;
        this.idUsuario     = idUsuario;
        this.usuarioEmail  = usuarioEmail;
        this.usuarioNombre = usuarioNombre;
        this.idProyecto    = idProyecto;
        this.idUsuarioObjetivo = idUsuarioObjetivo;
        this.fechaHora     = LocalDateTime.now();
    }

    // Getters y Setters
    public Long getId()                     { return id; }
    public String getAccion()               { return accion; }
    public void setAccion(String accion)    { this.accion = accion; }
    public String getDescripcion()          { return descripcion; }
    public void setDescripcion(String d)    { this.descripcion = d; }
    public Integer getIdUsuario()           { return idUsuario; }
    public void setIdUsuario(Integer id)    { this.idUsuario = id; }
    public Integer getIdUsuarioActor()      { return idUsuario; }
    @JsonIgnore
    public Usuario getUsuario()             { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public String getUsuarioEmail()         { return usuario != null && usuario.getEmail() != null ? usuario.getEmail() : usuarioEmail; }
    public void setUsuarioEmail(String e)   { this.usuarioEmail = e; }
    public String getUsuarioNombre()        { return usuario != null && usuario.getNombre() != null ? usuario.getNombre() : usuarioNombre; }
    public void setUsuarioNombre(String n)  { this.usuarioNombre = n; }
    public Integer getIdUsuarioObjetivo()   { return idUsuarioObjetivo; }
    public void setIdUsuarioObjetivo(Integer id) { this.idUsuarioObjetivo = id; }
    @JsonIgnore
    public Usuario getUsuarioObjetivo()     { return usuarioObjetivo; }
    public void setUsuarioObjetivo(Usuario usuarioObjetivo) { this.usuarioObjetivo = usuarioObjetivo; }
    public String getUsuarioObjetivoNombre() {
        return usuarioObjetivo != null ? usuarioObjetivo.getNombre() : null;
    }
    public String getUsuarioObjetivoEmail() {
        return usuarioObjetivo != null ? usuarioObjetivo.getEmail() : null;
    }
    public LocalDateTime getFechaHora()     { return fechaHora; }
    public void setFechaHora(LocalDateTime f) { this.fechaHora = f; }
    public Long getIdProyecto()             { return idProyecto; }
    public void setIdProyecto(Long id)      { this.idProyecto = id; }
    public String getExtra()                { return extra; }
    public void setExtra(String extra)      { this.extra = extra; }
}
